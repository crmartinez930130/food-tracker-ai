import type { MealAnalysis, MealAnalysisItem } from "./meal-analysis-schema.js";

/** Item names the model uses for summary rows (normalized: see normalizeName). */
const SUMMARY_NAMES = new Set([
  "total",
  "totals",
  "totales",
  "subtotal",
  "subtotals",
  "subtotales",
  "sum",
  "suma",
  "meal total",
  "total meal",
  "total comida",
  "total de la comida",
]);

/** Lowercase, no accents, no parenthesized text, letters only, single spaces. */
function normalizeName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/\([^)]*\)/g, " ")
    .replace(/[^\p{L}]+/gu, " ")
    .trim();
}

/** True when [name] is a summary word ("Total", "Subtotal:", "Total (meal)"), not a food. */
export function isSummaryName(name: string): boolean {
  return SUMMARY_NAMES.has(normalizeName(name));
}

const MACROS = ["calories", "proteinGrams", "fatGrams", "carbsGrams"] as const;

function isAllZero(item: MealAnalysisItem): boolean {
  return item.estimatedGrams === 0 && MACROS.every((key) => item[key] === 0);
}

/** Within 2 % or an absolute floor (5 kcal, 1 g), so rounding in the model's sum still matches. */
function closeTo(value: number, expected: number, floor: number): boolean {
  return Math.abs(value - expected) <= Math.max(floor, Math.abs(expected) * 0.02);
}

function repeatsSumOf(item: MealAnalysisItem, others: MealAnalysisItem[]): boolean {
  if (others.length === 0) return false;
  return MACROS.every((key) => {
    const sum = others.reduce((acc, other) => acc + other[key], 0);
    return closeTo(item[key], sum, key === "calories" ? 5 : 1);
  });
}

const round1 = (value: number) => Math.round(value * 10) / 10;

export interface SanitizedAnalysis {
  analysis: MealAnalysis;
  /** Names of the items removed as summary rows. */
  dropped: string[];
}

/**
 * Removes rows the model sometimes adds that aren't food, and recomputes the totals from the
 * remaining items so they always match the list:
 * - empty rows: 0 g and every value 0, whatever the name ("Total", blank, a repeated food);
 * - summary rows: a summary word ("Total", "Subtotal") whose values repeat the other items' sum.
 * A real food with 0 kcal keeps its grams (250 ml of water), so it's never empty.
 */
export function sanitizeAnalysis(analysis: MealAnalysis): SanitizedAnalysis {
  const dropped: string[] = [];
  const items = analysis.items.filter((item, index) => {
    const others = analysis.items.filter((_, otherIndex) => otherIndex !== index);
    const notFood = isAllZero(item) || (isSummaryName(item.name) && repeatsSumOf(item, others));
    if (notFood) dropped.push(item.name);
    return !notFood;
  });

  const sum = (key: (typeof MACROS)[number]) => round1(items.reduce((acc, item) => acc + item[key], 0));
  return {
    analysis: {
      ...analysis,
      items,
      totalCalories: sum("calories"),
      totalProteinGrams: sum("proteinGrams"),
      totalFatGrams: sum("fatGrams"),
      totalCarbsGrams: sum("carbsGrams"),
    },
    dropped,
  };
}
