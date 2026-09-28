import { describe, expect, it } from "vitest";
import { MealAnalysisSchema } from "../../src/services/meal-analysis-schema.js";

describe("MealAnalysisSchema", () => {
  it("accepts a well-formed analysis matching design.md's documented shape", () => {
    const sample = {
      items: [
        {
          name: "Grilled chicken breast",
          estimatedGrams: 150,
          confidence: "high",
          calories: 248,
          proteinGrams: 46.5,
          fatGrams: 5.4,
          carbsGrams: 0,
        },
      ],
      totalCalories: 248,
      totalProteinGrams: 46.5,
      totalFatGrams: 5.4,
      totalCarbsGrams: 0,
      notes: "Portion size estimated from plate diameter",
    };

    const result = MealAnalysisSchema.safeParse(sample);
    expect(result.success).toBe(true);
  });

  it("accepts an empty items list (no food detected)", () => {
    const result = MealAnalysisSchema.safeParse({
      items: [],
      totalCalories: 0,
      totalProteinGrams: 0,
      totalFatGrams: 0,
      totalCarbsGrams: 0,
    });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid confidence value", () => {
    const result = MealAnalysisSchema.safeParse({
      items: [
        {
          name: "Rice",
          estimatedGrams: 100,
          confidence: "very-high",
          calories: 130,
          proteinGrams: 2.7,
          fatGrams: 0.3,
          carbsGrams: 28,
        },
      ],
      totalCalories: 130,
      totalProteinGrams: 2.7,
      totalFatGrams: 0.3,
      totalCarbsGrams: 28,
    });
    expect(result.success).toBe(false);
  });
});
