import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { createMeal } from "../../src/db/meal-repository.js";
import { createWaterLog } from "../../src/db/water-repository.js";
import { rangeSummary } from "../../src/services/summary.js";
import type { MealItemInput } from "../../src/domain/meal.js";

const item: MealItemInput = {
  name: "Rice bowl",
  estimatedGrams: 300,
  confidence: "high",
  calories: 500,
  proteinGrams: 20,
  fatGrams: 10,
  carbsGrams: 80,
};

// Noon UTC keeps each meal on the same local calendar day in any test-machine timezone.
const at = (date: string) => `${date}T12:00:00.000Z`;

let db: AppDatabase;

beforeEach(() => {
  db = openDatabase(":memory:");
});

describe("rangeSummary", () => {
  it("returns one entry per day with that day's totals when every day has meals", () => {
    createMeal(db, { items: [item], occurredAt: at("2026-03-01") });
    createMeal(db, { items: [item, item], occurredAt: at("2026-03-02") });
    createMeal(db, { items: [item], occurredAt: at("2026-03-03") });

    const days = rangeSummary(db, "2026-03-01", "2026-03-03");

    expect(days.map((d) => d.date)).toEqual(["2026-03-01", "2026-03-02", "2026-03-03"]);
    expect(days.map((d) => d.totalCalories)).toEqual([500, 1000, 500]);
    expect(days[1]).toMatchObject({ totalProteinGrams: 40, totalFatGrams: 20, totalCarbsGrams: 160 });
  });

  it("zero-fills days without meals instead of omitting them", () => {
    createMeal(db, { items: [item], occurredAt: at("2026-03-02") });

    const days = rangeSummary(db, "2026-03-01", "2026-03-04");

    expect(days).toHaveLength(4);
    expect(days[0]).toEqual({
      date: "2026-03-01",
      totalCalories: 0,
      totalProteinGrams: 0,
      totalFatGrams: 0,
      totalCarbsGrams: 0,
      mealCount: 0,
      waterMl: 0,
    });
    expect(days[1].totalCalories).toBe(500);
    expect(days[3].totalCalories).toBe(0);
  });

  it("returns exactly one entry for a single-day range", () => {
    createMeal(db, { items: [item], occurredAt: at("2026-03-05") });

    expect(rangeSummary(db, "2026-03-05", "2026-03-05")).toEqual([
      { date: "2026-03-05", totalCalories: 500, totalProteinGrams: 20, totalFatGrams: 10, totalCarbsGrams: 80, mealCount: 1, waterMl: 0 },
    ]);
  });

  it("adds each day's meal count and water, including days with water but no meals", () => {
    createMeal(db, { items: [item], occurredAt: at("2026-03-01") });
    createMeal(db, { items: [item], occurredAt: at("2026-03-01") });
    createWaterLog(db, 500, at("2026-03-01"));
    createWaterLog(db, 250, at("2026-03-01"));
    createWaterLog(db, 500, at("2026-03-02"));

    const days = rangeSummary(db, "2026-03-01", "2026-03-03");

    expect(days.map((d) => [d.mealCount, d.waterMl])).toEqual([[2, 750], [0, 500], [0, 0]]);
    expect(days[1].totalCalories).toBe(0);
  });

  it("walks across a month boundary", () => {
    const days = rangeSummary(db, "2026-02-27", "2026-03-02");

    expect(days.map((d) => d.date)).toEqual(["2026-02-27", "2026-02-28", "2026-03-01", "2026-03-02"]);
  });
});
