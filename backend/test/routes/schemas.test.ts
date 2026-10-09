import { describe, expect, it } from "vitest";
import {
  SaveMealRequestSchema,
  UpdateGoalsRequestSchema,
  UpdateMealRequestSchema,
} from "../../src/routes/schemas.js";

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

describe("UpdateGoalsRequestSchema", () => {
  const validGoal = { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60, targetCarbsGrams: 170 };

  it("accepts a goal with all four targets", () => {
    expect(UpdateGoalsRequestSchema.safeParse(validGoal).success).toBe(true);
  });

  it("accepts zero macro targets", () => {
    const result = UpdateGoalsRequestSchema.safeParse({ ...validGoal, targetCarbsGrams: 0 });
    expect(result.success).toBe(true);
  });

  it.each(["targetCalories", "targetProteinGrams", "targetFatGrams", "targetCarbsGrams"] as const)(
    "rejects a goal missing %s",
    (field) => {
      const { [field]: _omitted, ...rest } = validGoal;
      expect(UpdateGoalsRequestSchema.safeParse(rest).success).toBe(false);
    },
  );

  it("rejects a non-numeric target", () => {
    expect(UpdateGoalsRequestSchema.safeParse({ ...validGoal, targetCalories: "1800" }).success).toBe(false);
  });

  it.each([0, -100])("rejects a calorie target of %d", (targetCalories) => {
    expect(UpdateGoalsRequestSchema.safeParse({ ...validGoal, targetCalories }).success).toBe(false);
  });

  it("rejects a negative macro target", () => {
    expect(UpdateGoalsRequestSchema.safeParse({ ...validGoal, targetFatGrams: -1 }).success).toBe(false);
  });
});
