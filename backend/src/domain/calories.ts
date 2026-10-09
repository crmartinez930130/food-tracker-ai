/** Atwater factors, in kcal per gram. */
const PROTEIN_KCAL_PER_GRAM = 4;
const FAT_KCAL_PER_GRAM = 9;
const CARBS_KCAL_PER_GRAM = 4;

export function caloriesFromMacros(proteinGrams: number, fatGrams: number, carbsGrams: number): number {
  if (proteinGrams < 0 || fatGrams < 0 || carbsGrams < 0) {
    throw new Error("Macro grams must not be negative");
  }
  return proteinGrams * PROTEIN_KCAL_PER_GRAM + fatGrams * FAT_KCAL_PER_GRAM + carbsGrams * CARBS_KCAL_PER_GRAM;
}
