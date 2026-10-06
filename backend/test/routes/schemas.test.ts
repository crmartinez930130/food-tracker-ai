import { describe, expect, it } from "vitest";
import { SaveMealRequestSchema, UpdateMealRequestSchema } from "../../src/routes/schemas.js";

const sampleItem = {
  name: "Grilled chicken breast",
  estimatedGrams: 150,
  confidence: "high" as const,
  calories: 248,
  proteinGrams: 46.5,
  fatGrams: 5.4,
  carbsGrams: 0,
};

describe("SaveMealRequestSchema mealType", () => {
  it("accepts each of the four valid meal types", () => {
    for (const mealType of ["breakfast", "lunch", "dinner", "snack"] as const) {
      const result = SaveMealRequestSchema.safeParse({ items: [sampleItem], mealType });
      expect(result.success).toBe(true);
    }
  });

  it("accepts a missing mealType", () => {
    const result = SaveMealRequestSchema.safeParse({ items: [sampleItem] });
    expect(result.success).toBe(true);
  });

  it("accepts an explicit null mealType", () => {
    const result = SaveMealRequestSchema.safeParse({ items: [sampleItem], mealType: null });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid mealType value", () => {
    const result = SaveMealRequestSchema.safeParse({ items: [sampleItem], mealType: "brunch" });
    expect(result.success).toBe(false);
  });
});

describe("UpdateMealRequestSchema mealType", () => {
  it("accepts a valid mealType", () => {
    const result = UpdateMealRequestSchema.safeParse({ mealType: "dinner" });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid mealType value", () => {
    const result = UpdateMealRequestSchema.safeParse({ mealType: "midnight-feast" });
    expect(result.success).toBe(false);
  });
});
