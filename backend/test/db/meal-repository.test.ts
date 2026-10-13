import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import {
  createMeal,
  dailyTotalsBetween,
  deleteMeal,
  exportRowsBetween,
  firstMealDate,
  getMealById,
  listMealsBetween,
  listFavoriteMeals,
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

describe("mealType", () => {
  it("persists the meal type when provided on save", () => {
    const entry = createMeal(db, { items: [sampleItem], mealType: "breakfast" });
    expect(entry.mealType).toBe("breakfast");
  });

  it("defaults to null when no meal type is provided on save", () => {
    const entry = createMeal(db, { items: [sampleItem] });
    expect(entry.mealType).toBeNull();
  });

  it("updates the meal type on edit", () => {
    const created = createMeal(db, { items: [sampleItem], mealType: "breakfast" });
    const updated = updateMeal(db, created.id, { mealType: "lunch" });
    expect(updated?.mealType).toBe("lunch");
  });

  it("reads back an entry with no meal type (as if saved before this change) without error", () => {
    const created = createMeal(db, { items: [sampleItem] });
    const fetched = getMealById(db, created.id);
    expect(fetched?.mealType).toBeNull();
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

describe("favorites", () => {
  it("saves new meals as not favorite", () => {
    expect(createMeal(db, { items: [sampleItem] }).isFavorite).toBe(false);
  });

  it("marks and unmarks a meal as favorite", () => {
    const created = createMeal(db, { items: [sampleItem] });

    expect(updateMeal(db, created.id, { isFavorite: true })?.isFavorite).toBe(true);
    expect(getMealById(db, created.id)?.isFavorite).toBe(true);

    expect(updateMeal(db, created.id, { isFavorite: false })?.isFavorite).toBe(false);
    expect(getMealById(db, created.id)?.isFavorite).toBe(false);
  });

  it("keeps the favorite flag when an update doesn't mention it", () => {
    const created = createMeal(db, { items: [sampleItem] });
    updateMeal(db, created.id, { isFavorite: true });

    expect(updateMeal(db, created.id, { notes: "edited" })?.isFavorite).toBe(true);
  });

  it("lists only favorites, from any date, newest first", () => {
    const older = createMeal(db, { items: [sampleItem], occurredAt: "2026-01-01T12:00:00.000Z" });
    const newer = createMeal(db, { items: [sampleItem], occurredAt: "2026-03-15T12:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-02-10T12:00:00.000Z" }); // not a favorite
    updateMeal(db, older.id, { isFavorite: true });
    updateMeal(db, newer.id, { isFavorite: true });

    const favorites = listFavoriteMeals(db);

    expect(favorites.map((m) => m.id)).toEqual([newer.id, older.id]);
    expect(favorites.every((m) => m.isFavorite)).toBe(true);
    expect(favorites[0].items[0].name).toBe(sampleItem.name);
  });

  it("returns an empty list when there are no favorites", () => {
    createMeal(db, { items: [sampleItem] });
    expect(listFavoriteMeals(db)).toEqual([]);
  });
});

describe("dailyTotalsBetween", () => {
  it("sums each day's meals, omits days without meals, and orders by day", () => {
    createMeal(db, { items: [sampleItem], occurredAt: "2026-03-03T12:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-03-01T12:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-03-01T13:00:00.000Z" });
    createMeal(db, { items: [sampleItem], occurredAt: "2026-03-10T12:00:00.000Z" }); // outside the range

    const days = dailyTotalsBetween(db, "2026-03-01", "2026-03-05");

    expect(days.map((d) => d.date)).toEqual(["2026-03-01", "2026-03-03"]);
    expect(days[0].totalCalories).toBe(2 * sampleItem.calories);
    expect(days[0].totalProteinGrams).toBeCloseTo(2 * sampleItem.proteinGrams);
    expect(days[1].totalCalories).toBe(sampleItem.calories);
    expect(days.map((d) => d.mealCount)).toEqual([2, 1]);
  });
});

describe("exportRowsBetween", () => {
  // Built from local wall-clock values, so the expected local date/time hold in any test timezone.
  const local = (y: number, m: number, d: number, h: number, min: number) =>
    new Date(y, m - 1, d, h, min).toISOString();
  const rice: MealItemInput = { ...sampleItem, name: "Rice", calories: 200 };

  it("returns one row per item with the meal's local date, time, type and notes", () => {
    createMeal(db, {
      items: [sampleItem, rice],
      occurredAt: local(2026, 3, 5, 13, 15),
      mealType: "lunch",
      notes: "office",
    });

    const rows = exportRowsBetween(db, "2026-03-05", "2026-03-06");

    expect(rows).toHaveLength(2);
    expect(rows.map((r) => r.item?.name)).toEqual(["Grilled chicken breast", "Rice"]);
    for (const r of rows) {
      expect(r).toMatchObject({ date: "2026-03-05", time: "13:15", mealType: "lunch", notes: "office" });
    }
    expect(rows[0].item).toEqual({
      name: "Grilled chicken breast",
      grams: 150,
      calories: 248,
      proteinGrams: 46.5,
      fatGrams: 5.4,
      carbsGrams: 0,
    });
  });

  it("gives a meal without items a single row with no item", () => {
    createMeal(db, { items: [], occurredAt: local(2026, 3, 5, 9, 0), notes: "forgot the photo" });

    expect(exportRowsBetween(db, "2026-03-05", "2026-03-06")).toEqual([
      { date: "2026-03-05", time: "09:00", mealType: null, notes: "forgot the photo", item: null },
    ]);
  });

  it("uses the local day for a late-evening meal", () => {
    createMeal(db, { items: [sampleItem], occurredAt: local(2026, 3, 5, 22, 30) });

    const rows = exportRowsBetween(db, "2026-03-05", "2026-03-06");
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ date: "2026-03-05", time: "22:30" });
    expect(exportRowsBetween(db, "2026-03-06", "2026-03-07")).toEqual([]);
  });

  it("orders rows chronologically regardless of insertion order, and respects the range bounds", () => {
    createMeal(db, { items: [rice], occurredAt: local(2026, 3, 6, 20, 0) });
    createMeal(db, { items: [rice], occurredAt: local(2026, 3, 4, 12, 0) }); // before the range
    createMeal(db, { items: [rice], occurredAt: local(2026, 3, 5, 8, 0) });
    createMeal(db, { items: [rice], occurredAt: local(2026, 3, 7, 8, 0) }); // end is exclusive

    const rows = exportRowsBetween(db, "2026-03-05", "2026-03-07");
    expect(rows.map((r) => `${r.date} ${r.time}`)).toEqual(["2026-03-05 08:00", "2026-03-06 20:00"]);
  });
});

describe("firstMealDate", () => {
  const local = (y: number, m: number, d: number, h: number) => new Date(y, m - 1, d, h).toISOString();

  it("is null when there are no meals", () => {
    expect(firstMealDate(db)).toBeNull();
  });

  it("returns the local day of the earliest meal, whatever the insertion order", () => {
    createMeal(db, { items: [sampleItem], occurredAt: local(2026, 3, 9, 12) });
    createMeal(db, { items: [sampleItem], occurredAt: local(2025, 11, 2, 12) });
    createMeal(db, { items: [sampleItem], occurredAt: local(2026, 1, 15, 12) });

    expect(firstMealDate(db)).toBe("2025-11-02");
  });

  it("uses the local day for a late-evening earliest meal", () => {
    createMeal(db, { items: [sampleItem], occurredAt: local(2026, 3, 5, 23) });

    expect(firstMealDate(db)).toBe("2026-03-05");
  });
});
