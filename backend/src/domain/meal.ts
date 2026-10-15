export type Confidence = "low" | "medium" | "high";
export type MealType = "breakfast" | "lunch" | "dinner" | "snack";

export interface MealItemInput {
  name: string;
  estimatedGrams: number;
  confidence: Confidence;
  calories: number;
  proteinGrams: number;
  fatGrams: number;
  carbsGrams: number;
}

export interface MealItem extends MealItemInput {
  id: number;
}

export interface MealTotals {
  totalCalories: number;
  totalProteinGrams: number;
  totalFatGrams: number;
  totalCarbsGrams: number;
}

export interface MealEntry extends MealTotals {
  id: number;
  occurredAt: string;
  createdAt: string;
  notes: string | null;
  mealType: MealType | null;
  isFavorite: boolean;
  /** The client's id for this meal, set when it was saved with one (see [SaveMealInput.clientId]). */
  clientId: string | null;
  items: MealItem[];
}

export interface SaveMealInput {
  items: MealItemInput[];
  notes?: string | null;
  occurredAt?: string | null;
  mealType?: MealType | null;
  /** Lower-case UUID the client generates per meal; a retried save with it returns the stored meal. */
  clientId?: string | null;
}

export interface UpdateMealInput {
  items?: MealItemInput[];
  notes?: string | null;
  occurredAt?: string | null;
  mealType?: MealType | null;
  isFavorite?: boolean;
}

export function computeTotals(items: MealItemInput[]): MealTotals {
  return items.reduce<MealTotals>(
    (totals, item) => ({
      totalCalories: totals.totalCalories + item.calories,
      totalProteinGrams: totals.totalProteinGrams + item.proteinGrams,
      totalFatGrams: totals.totalFatGrams + item.fatGrams,
      totalCarbsGrams: totals.totalCarbsGrams + item.carbsGrams,
    }),
    { totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
  );
}

/** One exported row: a meal item, or a whole meal (item fields null) when the meal has no items. */
export interface ExportRow {
  /** The meal's local calendar day, YYYY-MM-DD. */
  date: string;
  /** The meal's local time, HH:MM. */
  time: string;
  mealType: MealType | null;
  notes: string | null;
  item: {
    name: string;
    grams: number;
    calories: number;
    proteinGrams: number;
    fatGrams: number;
    carbsGrams: number;
  } | null;
}
