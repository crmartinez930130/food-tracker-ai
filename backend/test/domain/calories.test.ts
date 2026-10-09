import { describe, expect, it } from "vitest";
import { caloriesFromMacros } from "../../src/domain/calories.js";

describe("caloriesFromMacros", () => {
  it("applies 4 kcal/g for protein, 9 for fat and 4 for carbs", () => {
    expect(caloriesFromMacros(10, 10, 10)).toBe(170);
    expect(caloriesFromMacros(46.5, 5.4, 0)).toBeCloseTo(234.6);
  });

  it("returns 0 when all macros are 0", () => {
    expect(caloriesFromMacros(0, 0, 0)).toBe(0);
  });

  it.each([
    [-1, 0, 0],
    [0, -1, 0],
    [0, 0, -1],
  ])("rejects negative grams (%d, %d, %d)", (protein, fat, carbs) => {
    expect(() => caloriesFromMacros(protein, fat, carbs)).toThrow("must not be negative");
  });
});
