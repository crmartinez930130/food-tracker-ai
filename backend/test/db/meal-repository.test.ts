import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import {
  createMeal,
  deleteMeal,
  getMealById,
  listMealsBetween,
  listMealsByDate,
  updateMeal,
} from "../../src/db/meal-repository.js";
import type { MealItemInput } from "../../src/domain/meal.js";

const sampleItem: MealItemInput = {
  name: "Grilled chicken breast",
  estimatedGrams: 150,
  confidence: "high",
  calories: 248,
  proteinGrams: 46.5,
  fatGrams: 5.4,
  carbsGrams: 0,
};

let db: AppDatabase;

beforeEach(() => {
  db = openDatabase(":memory:");
});

describe("createMeal", () => {
  it("persists a meal with its items and computed totals", () => {
    const entry = createMeal(db, { items: [sampleItem] });

    expect(entry.id).toBeTypeOf("number");
    expect(entry.items).toHaveLength(1);
    expect(entry.items[0].name).toBe(sampleItem.name);
    expect(entry.totalCalories).toBe(248);
    expect(entry.totalProteinGrams).toBe(46.5);
  });

  it("defaults occurredAt to now when not specified", () => {
    const before = new Date().toISOString();
    const entry = createMeal(db, { items: [sampleItem] });
    expect(entry.occurredAt >= before).toBe(true);
  });

  it("persists an explicit occurredAt when provided", () => {
    const entry = createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T08:00:00.000Z" });
    expect(entry.occurredAt).toBe("2026-01-01T08:00:00.000Z");
  });
});

describe("getMealById", () => {
  it("returns null for a nonexistent id", () => {
    expect(getMealById(db, 999)).toBeNull();
  });

  it("returns the meal for an existing id", () => {
    const created = createMeal(db, { items: [sampleItem] });
    const fetched = getMealById(db, created.id);
    expect(fetched?.id).toBe(created.id);
  });
});

describe("listMealsByDate", () => {
  it("returns an empty list for a date with no meals", () => {
    expect(listMealsByDate(db, "2026-01-01")).toEqual([]);
  });

  it("returns meals saved on the given date", () => {
    // Times kept near noon UTC so the local calendar date is stable across server timezones.
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T10:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T14:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-02T12:00:00.000Z" });

    const results = listMealsByDate(db, "2026-01-01");
    expect(results).toHaveLength(2);
  });
});

describe("listMealsBetween", () => {
  it("returns meals within the inclusive start / exclusive end local date range", () => {
    // Noon UTC keeps the local calendar date stable across any reasonable server timezone.
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T12:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-07T12:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-01-08T12:00:00.000Z" });

    const results = listMealsBetween(db, "2026-01-01", "2026-01-08");
    expect(results).toHaveLength(2);
  });
});

describe("updateMeal", () => {
  it("returns null when updating a nonexistent meal", () => {
    expect(updateMeal(db, 999, { notes: "x" })).toBeNull();
  });

  it("persists corrected item values and recomputed totals", () => {
    const created = createMeal(db, { items: [sampleItem] });
    const corrected: MealItemInput = { ...sampleItem, estimatedGrams: 200, calories: 330 };

    const updated = updateMeal(db, created.id, { items: [corrected] });
    expect(updated?.items[0].estimatedGrams).toBe(200);
    expect(updated?.totalCalories).toBe(330);
  });

  it("leaves items untouched when only notes are updated", () => {
    const created = createMeal(db, { items: [sampleItem] });
    const updated = updateMeal(db, created.id, { notes: "extra sauce" });
    expect(updated?.notes).toBe("extra sauce");
    expect(updated?.items).toHaveLength(1);
  });
});

describe("deleteMeal", () => {
  it("removes the meal and its items", () => {
    const created = createMeal(db, { items: [sampleItem] });
    expect(deleteMeal(db, created.id)).toBe(true);
    expect(getMealById(db, created.id)).toBeNull();
  });

  it("returns false for a nonexistent meal", () => {
    expect(deleteMeal(db, 999)).toBe(false);
  });

  it("removes a deleted meal from date listings", () => {
    const created = createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T12:00:00.000Z" });
    deleteMeal(db, created.id);
    expect(listMealsByDate(db, "2026-01-01")).toEqual([]);
  });
});
