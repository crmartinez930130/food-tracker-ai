import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { getDailyGoal, saveDailyGoal } from "../../src/db/goals-repository.js";
import { goals } from "../../src/db/schema.js";

describe("goals repository", () => {
  let db: AppDatabase;

  beforeEach(() => {
    db = openDatabase(":memory:");
  });

  it("returns the built-in defaults, without persisting them, when no goal was saved", () => {
    expect(getDailyGoal(db)).toEqual({
      targetCalories: 2000,
      targetProteinGrams: 125,
      targetFatGrams: 67,
      targetCarbsGrams: 225,
      isDefault: true,
    });
    expect(db.select().from(goals).all()).toHaveLength(0);
  });

  it("saves a goal the first time and reads it back as not default", () => {
    const saved = saveDailyGoal(db, {
      targetCalories: 1800,
      targetProteinGrams: 140,
      targetFatGrams: 60,
      targetCarbsGrams: 170,
    });

    const expected = {
      targetCalories: 1800,
      targetProteinGrams: 140,
      targetFatGrams: 60,
      targetCarbsGrams: 170,
      isDefault: false,
    };
    expect(saved).toEqual(expected);
    expect(getDailyGoal(db)).toEqual(expected);
  });

  it("replaces an existing goal, keeping exactly one row", () => {
    saveDailyGoal(db, { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60, targetCarbsGrams: 170 });
    saveDailyGoal(db, { targetCalories: 2500, targetProteinGrams: 180, targetFatGrams: 80, targetCarbsGrams: 270 });

    expect(getDailyGoal(db)).toEqual({
      targetCalories: 2500,
      targetProteinGrams: 180,
      targetFatGrams: 80,
      targetCarbsGrams: 270,
      isDefault: false,
    });
    expect(db.select().from(goals).all()).toHaveLength(1);
  });

  it("keeps the saved goal when the database is reopened, as after a backend restart", () => {
    const dir = mkdtempSync(join(tmpdir(), "goals-test-"));
    try {
      const dbPath = join(dir, "food-tracker.sqlite");
      saveDailyGoal(openDatabase(dbPath), {
        targetCalories: 1800,
        targetProteinGrams: 140,
        targetFatGrams: 60,
        targetCarbsGrams: 170,
      });

      expect(getDailyGoal(openDatabase(dbPath))).toMatchObject({ targetCalories: 1800, isDefault: false });
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });
});
