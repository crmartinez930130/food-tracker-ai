import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { getDailyGoal, saveDailyGoal } from "../../src/db/goals-repository.js";
import { sql } from "drizzle-orm";
import { goals } from "../../src/db/schema.js";

const nutrition = { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60, targetCarbsGrams: 170 };

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
      targetWaterMl: 2000,
      waterGlassMl: 250,
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
      // Not sent, so the water settings keep their defaults.
      targetWaterMl: 2000,
      waterGlassMl: 250,
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
      targetWaterMl: 2000,
      waterGlassMl: 250,
      isDefault: false,
    });
    expect(db.select().from(goals).all()).toHaveLength(1);
  });

  it("saves the water target and glass size when given", () => {
    const saved = saveDailyGoal(db, { ...nutrition, targetWaterMl: 2500, waterGlassMl: 330 });

    expect(saved).toMatchObject({ targetWaterMl: 2500, waterGlassMl: 330, isDefault: false });
    expect(getDailyGoal(db)).toMatchObject({ targetWaterMl: 2500, waterGlassMl: 330 });
  });

  it("keeps the stored water settings when a save omits them", () => {
    saveDailyGoal(db, { ...nutrition, targetWaterMl: 2500, waterGlassMl: 330 });

    const saved = saveDailyGoal(db, { ...nutrition, targetCalories: 2200 });

    expect(saved).toMatchObject({ targetCalories: 2200, targetWaterMl: 2500, waterGlassMl: 330 });
  });

  it("keeps one stored water setting when only the other is sent", () => {
    saveDailyGoal(db, { ...nutrition, targetWaterMl: 2500, waterGlassMl: 330 });

    expect(saveDailyGoal(db, { ...nutrition, waterGlassMl: 500 })).toMatchObject({
      targetWaterMl: 2500,
      waterGlassMl: 500,
    });
  });

  it("gives a goal row written without the water columns their defaults", () => {
    // Like a goal saved before migration 0005 added them.
    db.run(sql`INSERT INTO goals (id, target_calories, target_protein_grams, target_fat_grams, target_carbs_grams, updated_at)
               VALUES (1, 1800, 140, 60, 170, '2026-10-01T00:00:00.000Z')`);

    expect(getDailyGoal(db)).toEqual({ ...nutrition, targetWaterMl: 2000, waterGlassMl: 250, isDefault: false });
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
