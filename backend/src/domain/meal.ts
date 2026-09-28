export type Confidence = "low" | "medium" | "high";

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
  items: MealItem[];
}

export interface SaveMealInput {
  items: MealItemInput[];
  notes?: string | null;
  occurredAt?: string | null;
}

export interface UpdateMealInput {
  items?: MealItemInput[];
  notes?: string | null;
  occurredAt?: string | null;
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
