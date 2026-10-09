import { eq } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { goals, GOALS_ROW_ID } from "./schema.js";
import { DEFAULT_DAILY_GOAL, type DailyGoal, type DailyGoalInput } from "../domain/goals.js";

/** The stored goal, or the built-in defaults (not persisted) when none has been saved. */
export function getDailyGoal(db: AppDatabase): DailyGoal {
  const row = db.select().from(goals).where(eq(goals.id, GOALS_ROW_ID)).get();
  if (!row) {
    return { ...DEFAULT_DAILY_GOAL, isDefault: true };
  }
  return {
    targetCalories: row.targetCalories,
    targetProteinGrams: row.targetProteinGrams,
    targetFatGrams: row.targetFatGrams,
    targetCarbsGrams: row.targetCarbsGrams,
    isDefault: false,
  };
}

/** Creates the goal or replaces the existing one - there is never more than one row. */
export function saveDailyGoal(db: AppDatabase, input: DailyGoalInput): DailyGoal {
  const values = {
    targetCalories: input.targetCalories,
    targetProteinGrams: input.targetProteinGrams,
    targetFatGrams: input.targetFatGrams,
    targetCarbsGrams: input.targetCarbsGrams,
    updatedAt: new Date().toISOString(),
  };
  db.insert(goals)
    .values({ id: GOALS_ROW_ID, ...values })
    .onConflictDoUpdate({ target: goals.id, set: values })
    .run();
  return getDailyGoal(db);
}
