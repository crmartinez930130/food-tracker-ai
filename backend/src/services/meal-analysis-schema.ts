import { z } from "zod";

export const MealAnalysisItemSchema = z.object({
  name: z.string(),
  estimatedGrams: z.number(),
  confidence: z.enum(["low", "medium", "high"]),
  calories: z.number(),
  proteinGrams: z.number(),
  fatGrams: z.number(),
  carbsGrams: z.number(),
});

export const MealAnalysisSchema = z.object({
  items: z.array(MealAnalysisItemSchema),
  totalCalories: z.number(),
  totalProteinGrams: z.number(),
  totalFatGrams: z.number(),
  totalCarbsGrams: z.number(),
  notes: z.string().optional(),
});

export type MealAnalysisItem = z.infer<typeof MealAnalysisItemSchema>;
export type MealAnalysis = z.infer<typeof MealAnalysisSchema>;
