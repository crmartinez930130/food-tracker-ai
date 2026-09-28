import { eq, sql } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { meals, mealItems, type NewMealItemRow } from "./schema.js";
import {
  computeTotals,
  type MealEntry,
  type MealItem,
  type SaveMealInput,
  type UpdateMealInput,
} from "../domain/meal.js";

function toMealEntry(
  mealRow: typeof meals.$inferSelect,
  itemRows: (typeof mealItems.$inferSelect)[],
): MealEntry {
  const items: MealItem[] = itemRows.map((row) => ({
    id: row.id,
    name: row.name,
    estimatedGrams: row.estimatedGrams,
    confidence: row.confidence,
    calories: row.calories,
    proteinGrams: row.proteinGrams,
    fatGrams: row.fatGrams,
    carbsGrams: row.carbsGrams,
  }));

  return {
    id: mealRow.id,
    occurredAt: mealRow.occurredAt,
    createdAt: mealRow.createdAt,
    notes: mealRow.notes,
    items,
    totalCalories: mealRow.totalCalories,
    totalProteinGrams: mealRow.totalProteinGrams,
    totalFatGrams: mealRow.totalFatGrams,
    totalCarbsGrams: mealRow.totalCarbsGrams,
  };
}

function itemRowsFor(mealId: number, input: SaveMealInput["items"]): NewMealItemRow[] {
  return input.map((item) => ({
    mealId,
    name: item.name,
    estimatedGrams: item.estimatedGrams,
    confidence: item.confidence,
    calories: item.calories,
    proteinGrams: item.proteinGrams,
    fatGrams: item.fatGrams,
    carbsGrams: item.carbsGrams,
  }));
}

export function createMeal(db: AppDatabase, input: SaveMealInput): MealEntry {
  const now = new Date().toISOString();
  const totals = computeTotals(input.items);

  const [mealRow] = db
    .insert(meals)
    .values({
      occurredAt: input.occurredAt ?? now,
      createdAt: now,
      notes: input.notes ?? null,
      ...totals,
    })
    .returning()
    .all();

  if (input.items.length > 0) {
    db.insert(mealItems).values(itemRowsFor(mealRow.id, input.items)).run();
  }

  return getMealById(db, mealRow.id)!;
}

export function getMealById(db: AppDatabase, id: number): MealEntry | null {
  const mealRow = db.select().from(meals).where(eq(meals.id, id)).get();
  if (!mealRow) return null;

  const itemRows = db.select().from(mealItems).where(eq(mealItems.mealId, id)).all();
  return toMealEntry(mealRow, itemRows);
}

export function listMealsByDate(db: AppDatabase, date: string): MealEntry[] {
  const mealRows = db
    .select()
    .from(meals)
    .where(sql`date(${meals.occurredAt}) = ${date}`)
    .all();

  return mealRows.map((mealRow) => {
    const itemRows = db.select().from(mealItems).where(eq(mealItems.mealId, mealRow.id)).all();
    return toMealEntry(mealRow, itemRows);
  });
}

export function listMealsBetween(db: AppDatabase, startIsoInclusive: string, endIsoExclusive: string): MealEntry[] {
  const mealRows = db
    .select()
    .from(meals)
    .where(sql`${meals.occurredAt} >= ${startIsoInclusive} AND ${meals.occurredAt} < ${endIsoExclusive}`)
    .all();

  return mealRows.map((mealRow) => {
    const itemRows = db.select().from(mealItems).where(eq(mealItems.mealId, mealRow.id)).all();
    return toMealEntry(mealRow, itemRows);
  });
}

export function updateMeal(db: AppDatabase, id: number, input: UpdateMealInput): MealEntry | null {
  const existing = getMealById(db, id);
  if (!existing) return null;

  const items = input.items ?? existing.items;
  const totals = computeTotals(items);

  db.update(meals)
    .set({
      occurredAt: input.occurredAt ?? existing.occurredAt,
      notes: input.notes !== undefined ? input.notes : existing.notes,
      ...totals,
    })
    .where(eq(meals.id, id))
    .run();

  if (input.items) {
    db.delete(mealItems).where(eq(mealItems.mealId, id)).run();
    if (input.items.length > 0) {
      db.insert(mealItems).values(itemRowsFor(id, input.items)).run();
    }
  }

  return getMealById(db, id);
}

export function deleteMeal(db: AppDatabase, id: number): boolean {
  const result = db.delete(meals).where(eq(meals.id, id)).run();
  return result.changes > 0;
}
