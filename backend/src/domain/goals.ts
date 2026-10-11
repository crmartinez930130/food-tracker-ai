export interface DailyGoalInput {
  targetCalories: number;
  targetProteinGrams: number;
  targetFatGrams: number;
  targetCarbsGrams: number;
  /** Daily water target, in ml. */
  targetWaterMl: number;
  /** How much one tap of "add a glass" logs, in ml. */
  waterGlassMl: number;
}

/** What a save may send: the water settings are optional and keep their current value when omitted. */
export type DailyGoalUpdate = Omit<DailyGoalInput, "targetWaterMl" | "waterGlassMl"> &
  Partial<Pick<DailyGoalInput, "targetWaterMl" | "waterGlassMl">>;

export interface DailyGoal extends DailyGoalInput {
  /** True when the user has never saved a goal and these are the built-in defaults. */
  isDefault: boolean;
}

/** 2000 kcal split 25/30/45 % protein/fat/carbs (fat rounded from 66.7 g), 2 l of water in 250 ml glasses. */
export const DEFAULT_DAILY_GOAL: DailyGoalInput = {
  targetCalories: 2000,
  targetProteinGrams: 125,
  targetFatGrams: 67,
  targetCarbsGrams: 225,
  targetWaterMl: 2000,
  waterGlassMl: 250,
};
