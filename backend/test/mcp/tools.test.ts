import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { callOk, callTool, connectMcpClient, textOf, type McpTestContext } from "../helpers/mcp-client.js";
import { createMeal, updateMeal } from "../../src/db/meal-repository.js";
import { saveDailyGoal } from "../../src/db/goals-repository.js";
import { createWeightLog } from "../../src/db/weight-repository.js";
import { createWaterLog } from "../../src/db/water-repository.js";
import { DEFAULT_DAILY_GOAL } from "../../src/domain/goals.js";
import { localToday } from "../../src/routes/dates.js";
import type { MealItemInput } from "../../src/domain/meal.js";

/** A local wall-clock time as the stored UTC ISO timestamp, so the tests hold in any timezone. */
const local = (y: number, m: number, d: number, h: number, min = 0) => new Date(y, m - 1, d, h, min).toISOString();

const rice: MealItemInput = {
  name: "Arroz",
  estimatedGrams: 150,
  confidence: "high",
  calories: 200,
  proteinGrams: 4,
  fatGrams: 1,
  carbsGrams: 44,
};
const chicken: MealItemInput = {
  name: "Pollo",
  estimatedGrams: 120,
  confidence: "medium",
  calories: 240,
  proteinGrams: 36,
  fatGrams: 10,
  carbsGrams: 0,
};

let ctx: McpTestContext;
beforeEach(async () => {
  ctx = await connectMcpClient();
});
afterEach(async () => {
  await ctx.close();
});

async function expectToolError(name: string, args: Record<string, unknown>, message: string | RegExp) {
  const result = await callTool(ctx.client, name, args);
  expect(result.isError, `${name} should fail`).toBe(true);
  expect(textOf(result)).toMatch(message);
}

describe("get_goals", () => {
  it("returns the built-in defaults when no goal was saved", async () => {
    expect(await callOk(ctx.client, "get_goals")).toEqual({ ...DEFAULT_DAILY_GOAL, isDefault: true });
  });

  it("returns the saved goals", async () => {
    saveDailyGoal(ctx.db, { targetCalories: 2400, targetProteinGrams: 160, targetFatGrams: 70, targetCarbsGrams: 280, targetWaterMl: 3000 });
    expect(await callOk(ctx.client, "get_goals")).toMatchObject({ targetCalories: 2400, targetWaterMl: 3000, isDefault: false });
  });
});

describe("get_data_range", () => {
  it("has no first day when nothing was logged, and today is the server's local date", async () => {
    expect(await callOk(ctx.client, "get_data_range")).toEqual({ firstMealDate: null, today: localToday() });
  });

  it("returns the local day of the earliest meal", async () => {
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 9, 12) });
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 2, 1, 22, 30) });
    expect(await callOk(ctx.client, "get_data_range")).toMatchObject({ firstMealDate: "2026-02-01" });
  });
});

describe("get_daily_summary", () => {
  it("adds up the day's meals and water, next to the goals", async () => {
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 5, 13) });
    createMeal(ctx.db, { items: [chicken], occurredAt: local(2026, 3, 5, 22, 30) }); // evening: still the local day
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 6, 1) }); // next day
    createWaterLog(ctx.db, 250, local(2026, 3, 5, 9));
    createWaterLog(ctx.db, 250, local(2026, 3, 5, 23));

    expect(await callOk(ctx.client, "get_daily_summary", { date: "2026-03-05" })).toEqual({
      date: "2026-03-05",
      totalCalories: 440,
      totalProteinGrams: 40,
      totalFatGrams: 11,
      totalCarbsGrams: 44,
      mealCount: 2,
      waterMl: 500,
      goals: { ...DEFAULT_DAILY_GOAL, isDefault: true },
    });
  });

  it("returns zeros for a day without data", async () => {
    expect(await callOk(ctx.client, "get_daily_summary", { date: "2026-03-05" })).toMatchObject({
      totalCalories: 0,
      mealCount: 0,
      waterMl: 0,
    });
  });

  it.each(["2026-02-30", "05/03/2026", "2026-3-5"])("rejects the date %s", async (date) => {
    await expectToolError("get_daily_summary", { date }, "'date' must be a real date in YYYY-MM-DD format");
  });

  it("rejects a missing date", async () => {
    const result = await callTool(ctx.client, "get_daily_summary", {});
    expect(result.isError).toBe(true);
  });
});

describe("get_range_summary", () => {
  it("returns one zero-filled entry per day", async () => {
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 2, 13) });
    createWaterLog(ctx.db, 300, local(2026, 3, 3, 10));

    const { days } = await callOk<{ days: Array<Record<string, unknown>> }>(ctx.client, "get_range_summary", {
      startDate: "2026-03-01",
      endDate: "2026-03-03",
    });
    expect(days.map((d) => [d.date, d.totalCalories, d.mealCount, d.waterMl])).toEqual([
      ["2026-03-01", 0, 0, 0],
      ["2026-03-02", 200, 1, 0],
      ["2026-03-03", 0, 0, 300],
    ]);
  });

  it("accepts a full year and rejects a longer range", async () => {
    await callOk(ctx.client, "get_range_summary", { startDate: "2026-01-01", endDate: "2027-01-01" });
    await expectToolError("get_range_summary", { startDate: "2026-01-01", endDate: "2027-01-02" }, "can't be longer than 366 days");
  });

  it("rejects an end before the start", async () => {
    await expectToolError("get_range_summary", { startDate: "2026-03-05", endDate: "2026-03-04" }, "must not be before");
  });

  it("rejects an impossible end date", async () => {
    await expectToolError("get_range_summary", { startDate: "2026-02-01", endDate: "2026-02-30" }, "'endDate' must be a real date");
  });
});

describe("list_weight_logs", () => {
  it("lists the weighings in the range, oldest first", async () => {
    createWeightLog(ctx.db, { weightKg: 73.0, day: "2026-03-03" }, "2026-03-03T07:00:00.000Z");
    createWeightLog(ctx.db, { weightKg: 72.0, day: "2026-03-01" }, "2026-03-01T07:00:00.000Z");
    createWeightLog(ctx.db, { weightKg: 71.0, day: "2026-02-28" }, "2026-02-28T07:00:00.000Z");

    const { logs } = await callOk<{ logs: Array<{ day: string; weightKg: number; id: number; createdAt: string }> }>(
      ctx.client,
      "list_weight_logs",
      { startDate: "2026-03-01", endDate: "2026-03-31" },
    );
    expect(logs.map((l) => [l.day, l.weightKg])).toEqual([
      ["2026-03-01", 72.0],
      ["2026-03-03", 73.0],
    ]);
    expect(logs[0]).toMatchObject({ id: expect.any(Number), createdAt: "2026-03-01T07:00:00.000Z" });
  });

  it("rejects a range over 366 days", async () => {
    await expectToolError("list_weight_logs", { startDate: "2025-01-01", endDate: "2026-03-01" }, "366 days");
  });
});

describe("list_meals", () => {
  it("returns the meals with their items, oldest first", async () => {
    createMeal(ctx.db, { items: [chicken], occurredAt: local(2026, 3, 6, 20), mealType: "dinner", notes: "con ensalada" });
    createMeal(ctx.db, { items: [rice, chicken], occurredAt: local(2026, 3, 5, 13), mealType: "lunch" });
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 8, 13), mealType: "lunch" }); // after the range

    const { meals } = await callOk<{ meals: Array<Record<string, any>> }>(ctx.client, "list_meals", {
      startDate: "2026-03-05",
      endDate: "2026-03-07",
    });
    expect(meals.map((m) => m.mealType)).toEqual(["lunch", "dinner"]);
    expect(meals[0]).toMatchObject({ totalCalories: 440, isFavorite: false });
    expect(meals[0].items.map((i: { name: string }) => i.name)).toEqual(["Arroz", "Pollo"]);
    expect(meals[0].items[0]).toMatchObject({ estimatedGrams: 150, confidence: "high", calories: 200, proteinGrams: 4 });
    expect(meals[1]).toMatchObject({ notes: "con ensalada" });
  });

  it("filters by meal type", async () => {
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 5, 8), mealType: "breakfast" });
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 5, 13), mealType: "lunch" });
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 6, 6, 30), mealType: "pre_workout" });

    const { meals } = await callOk<{ meals: Array<{ mealType: string }> }>(ctx.client, "list_meals", {
      startDate: "2026-03-01",
      endDate: "2026-03-07",
      mealType: "pre_workout",
    });
    expect(meals.map((m) => m.mealType)).toEqual(["pre_workout"]);
  });

  it("rejects an unknown meal type", async () => {
    const result = await callTool(ctx.client, "list_meals", { startDate: "2026-03-01", endDate: "2026-03-07", mealType: "brunch" });
    expect(result.isError).toBe(true);
  });

  it("accepts 31 days and rejects 32", async () => {
    await callOk(ctx.client, "list_meals", { startDate: "2026-03-01", endDate: "2026-03-31" });
    await expectToolError("list_meals", { startDate: "2026-03-01", endDate: "2026-04-01" }, "can't be longer than 31 days");
  });
});

describe("get_meal", () => {
  it("returns one meal with its items", async () => {
    const meal = createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 5, 13), mealType: "lunch" });
    expect(await callOk(ctx.client, "get_meal", { id: meal.id })).toMatchObject({
      id: meal.id,
      mealType: "lunch",
      items: [expect.objectContaining({ name: "Arroz" })],
    });
  });

  it("reports an unknown meal", async () => {
    await expectToolError("get_meal", { id: 999 }, "Meal 999 not found");
  });

  it.each([0, -1, 1.5])("rejects the id %s", async (id) => {
    const result = await callTool(ctx.client, "get_meal", { id });
    expect(result.isError).toBe(true);
  });
});

describe("list_favorite_meals", () => {
  it("returns only favorites, newest first", async () => {
    const older = createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 1, 13) });
    const newer = createMeal(ctx.db, { items: [chicken], occurredAt: local(2026, 3, 5, 13) });
    createMeal(ctx.db, { items: [rice], occurredAt: local(2026, 3, 6, 13) });
    updateMeal(ctx.db, older.id, { isFavorite: true });
    updateMeal(ctx.db, newer.id, { isFavorite: true });

    const { meals } = await callOk<{ meals: Array<{ id: number; isFavorite: boolean }> }>(ctx.client, "list_favorite_meals");
    expect(meals.map((m) => m.id)).toEqual([newer.id, older.id]);
  });
});

describe("list_water_logs", () => {
  it("groups the entries per local day with their totals", async () => {
    createWaterLog(ctx.db, 250, local(2026, 3, 1, 9));
    createWaterLog(ctx.db, 500, local(2026, 3, 1, 23, 30));
    createWaterLog(ctx.db, 300, local(2026, 3, 3, 12));
    createWaterLog(ctx.db, 999, local(2026, 3, 4, 12)); // after the range

    const { days } = await callOk<{ days: Array<{ date: string; totalMl: number; entries: Array<Record<string, unknown>> }> }>(
      ctx.client,
      "list_water_logs",
      { startDate: "2026-03-01", endDate: "2026-03-03" },
    );
    expect(days.map((d) => [d.date, d.totalMl, d.entries.map((e) => e.amountMl)])).toEqual([
      ["2026-03-01", 750, [250, 500]],
      ["2026-03-03", 300, [300]],
    ]);
    expect(days[0].entries[0]).toEqual({ id: expect.any(Number), amountMl: 250, loggedAt: local(2026, 3, 1, 9) });
  });

  it("rejects a range over 31 days", async () => {
    await expectToolError("list_water_logs", { startDate: "2026-03-01", endDate: "2026-04-01" }, "31 days");
  });
});

describe("read-only", () => {
  it("leaves every table unchanged after calling every tool with valid and invalid arguments", async () => {
    const meal = createMeal(ctx.db, { items: [rice, chicken], occurredAt: local(2026, 3, 5, 13), mealType: "lunch" });
    updateMeal(ctx.db, meal.id, { isFavorite: true });
    saveDailyGoal(ctx.db, { targetCalories: 2400, targetProteinGrams: 160, targetFatGrams: 70, targetCarbsGrams: 280 });
    createWeightLog(ctx.db, { weightKg: 72.0, day: "2026-03-05" });
    createWaterLog(ctx.db, 250, local(2026, 3, 5, 9));

    const snapshot = () =>
      Object.fromEntries(
        ["meals", "meal_items", "goals", "weight_logs", "water_logs"].map((table) => [
          table,
          ctx.db.$client.prepare(`SELECT * FROM ${table} ORDER BY id`).all(),
        ]),
      );
    const before = snapshot();

    const range = { startDate: "2026-03-01", endDate: "2026-03-07" };
    const calls: Array<[string, Record<string, unknown>]> = [
      ["get_goals", {}],
      ["get_data_range", {}],
      ["get_daily_summary", { date: "2026-03-05" }],
      ["get_daily_summary", { date: "nope" }],
      ["get_range_summary", range],
      ["get_range_summary", { startDate: "2026-03-07", endDate: "2026-03-01" }],
      ["list_meals", { ...range, mealType: "lunch" }],
      ["list_meals", { startDate: "2026-01-01", endDate: "2026-12-31" }],
      ["get_meal", { id: meal.id }],
      ["get_meal", { id: 999 }],
      ["list_favorite_meals", {}],
      ["list_weight_logs", range],
      ["list_water_logs", range],
      ["list_water_logs", { startDate: "x", endDate: "y" }],
    ];
    for (const [name, args] of calls) await callTool(ctx.client, name, args);

    expect(snapshot()).toEqual(before);
  });
});
