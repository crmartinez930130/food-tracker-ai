export interface DailyGoalInput {
  targetCalories: number;
  targetProteinGrams: number;
  targetFatGrams: number;
  targetCarbsGrams: number;
}

export interface DailyGoal extends DailyGoalInput {
  /** True when the user has never saved a goal and these are the built-in defaults. */
  isDefault: boolean;
}

/** 2000 kcal split 25/30/45 % protein/fat/carbs (fat rounded from 66.7 g). */
export const DEFAULT_DAILY_GOAL: DailyGoalInput = {
  targetCalories: 2000,
  targetProteinGrams: 125,
  targetFatGrams: 67,
  targetCarbsGrams: 225,
};
