import { describe, expect, it } from "vitest";
import { isSummaryName, sanitizeAnalysis } from "../../src/services/analysis-sanitizer.js";
import type { MealAnalysis, MealAnalysisItem } from "../../src/services/meal-analysis-schema.js";

const food = (name: string, calories: number, protein = 0, fat = 0, carbs = 0, grams = 100): MealAnalysisItem => ({
  name,
  estimatedGrams: grams,
  confidence: "high",
  calories,
  proteinGrams: protein,
  fatGrams: fat,
  carbsGrams: carbs,
});

const analysisOf = (items: MealAnalysisItem[]): MealAnalysis => ({
  items,
  totalCalories: 999,
  totalProteinGrams: 999,
  totalFatGrams: 999,
  totalCarbsGrams: 999,
  notes: "kept",
});

const banana = food("Banana", 105, 1.3, 0.4, 27, 118);
const milk = food("Milk (2% reduced fat), 1 glass", 122, 8.1, 4.8, 12, 244);

describe("isSummaryName", () => {
  it("recognizes summary words in English and Spanish, ignoring case, accents and punctuation", () => {
    for (const name of ["Total", "TOTAL:", "total (meal)", "Totales", "Subtotal", "Sum", "Suma", "Meal total", "Total de la comida", "  total  "]) {
      expect(isSummaryName(name), name).toBe(true);
    }
  });

  it("does not take real foods for summaries", () => {
    for (const name of ["Total cereal", "Water", "Agua", "Totopos", "Sumac", "Banana"]) {
      expect(isSummaryName(name), name).toBe(false);
    }
  });
});

describe("sanitizeAnalysis", () => {
  it("drops an all-zero Total row and recomputes the totals", () => {
    const { analysis, dropped } = sanitizeAnalysis(analysisOf([banana, milk, food("Total", 0, 0, 0, 0, 0)]));

    expect(analysis.items.map((item) => item.name)).toEqual([banana.name, milk.name]);
    expect(dropped).toEqual(["Total"]);
    expect(analysis).toMatchObject({ totalCalories: 227, totalProteinGrams: 9.4, totalFatGrams: 5.2, totalCarbsGrams: 39 });
    expect(analysis.notes).toBe("kept");
  });

  it("drops a Total row that repeats the sum of the others, within tolerance", () => {
    const total = food("Total:", 228, 9.4, 5.2, 39.5, 362);

    const { analysis, dropped } = sanitizeAnalysis(analysisOf([banana, milk, total]));

    expect(dropped).toEqual(["Total:"]);
    expect(analysis.totalCalories).toBe(227);
  });

  it("keeps an item named Total whose values don't look like a summary", () => {
    const odd = food("Total", 300, 20, 10, 30);

    const { analysis, dropped } = sanitizeAnalysis(analysisOf([banana, odd]));

    expect(dropped).toEqual([]);
    expect(analysis.items).toHaveLength(2);
    expect(analysis.totalCalories).toBe(405);
  });

  it("drops empty rows whatever their name: blank or repeating another food", () => {
    const blank = food("", 0, 0, 0, 0, 0);
    const repeated = food("Vaso de leche entera", 0, 0, 0, 0, 0);

    const { analysis, dropped } = sanitizeAnalysis(analysisOf([banana, food("Vaso de leche entera", 155, 8, 8.2, 12, 250), repeated, blank]));

    expect(analysis.items.map((item) => item.name)).toEqual(["Banana", "Vaso de leche entera"]);
    expect(dropped).toEqual(["Vaso de leche entera", ""]);
    expect(analysis.totalCalories).toBe(260);
  });

  it("keeps real foods with zero calories", () => {
    const water = food("Water", 0, 0, 0, 0, 250);

    const { analysis, dropped } = sanitizeAnalysis(analysisOf([banana, water]));

    expect(dropped).toEqual([]);
    expect(analysis.items.map((item) => item.name)).toEqual(["Banana", "Water"]);
  });

  it("always recomputes the totals, rounded to one decimal", () => {
    const { analysis } = sanitizeAnalysis(analysisOf([food("A", 0.1, 0.1, 0.2, 0.3), food("B", 0.2, 0.2, 0.1, 0.3)]));

    expect(analysis).toMatchObject({ totalCalories: 0.3, totalProteinGrams: 0.3, totalFatGrams: 0.3, totalCarbsGrams: 0.6 });
  });

  it("handles an empty list", () => {
    const { analysis, dropped } = sanitizeAnalysis(analysisOf([]));

    expect(analysis.items).toEqual([]);
    expect(analysis.totalCalories).toBe(0);
    expect(dropped).toEqual([]);
  });
});
