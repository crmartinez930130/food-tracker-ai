import { desc, eq, sql } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { meals, mealItems, type NewMealItemRow } from "./schema.js";
import {
  computeTotals,
  type MealEntry,
  type MealItem,
  type MealTotals,
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
    mealType: mealRow.mealType,
    isFavorite: mealRow.isFavorite,
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
      mealType: input.mealType ?? null,
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

// occurredAt is stored as a UTC ISO timestamp, but "which day" a meal belongs to must match
// the user's own calendar day. This is a single-user, single-machine app (see design.md), so
// the backend's local timezone *is* the user's timezone - 'localtime' converts the stored UTC
// timestamp before comparing, instead of grouping by the UTC calendar date (which silently
// shifts meals logged in the evening onto the wrong day for any timezone behind UTC).
export function listMealsByDate(db: AppDatabase, date: string): MealEntry[] {
  const mealRows = db
    .select()
    .from(meals)
    .where(sql`date(${meals.occurredAt}, 'localtime') = ${date}`)
    .all();

  return mealRows.map((mealRow) => {
    const itemRows = db.select().from(mealItems).where(eq(mealItems.mealId, mealRow.id)).all();
    return toMealEntry(mealRow, itemRows);
  });
}

export function listMealsBetween(db: AppDatabase, startDateInclusive: string, endDateExclusive: string): MealEntry[] {
  const mealRows = db
    .select()
    .from(meals)
    .where(
      sql`date(${meals.occurredAt}, 'localtime') >= ${startDateInclusive} AND date(${meals.occurredAt}, 'localtime') < ${endDateExclusive}`,
    )
    .all();

  return mealRows.map((mealRow) => {
    const itemRows = db.select().from(mealItems).where(eq(mealItems.mealId, mealRow.id)).all();
    return toMealEntry(mealRow, itemRows);
  });
}

export interface DayTotals extends MealTotals {
  date: string;
}

/**
 * Summed totals per local calendar day in [startDateInclusive, endDateExclusive), only for days
 * that have meals, in date order. Aggregates in SQL so items don't need to be loaded.
 */
export function dailyTotalsBetween(db: AppDatabase, startDateInclusive: string, endDateExclusive: string): DayTotals[] {
  const day = sql<string>`date(${meals.occurredAt}, 'localtime')`;
  return db
    .select({
      date: day,
      totalCalories: sql<number>`sum(${meals.totalCalories})`,
      totalProteinGrams: sql<number>`sum(${meals.totalProteinGrams})`,
      totalFatGrams: sql<number>`sum(${meals.totalFatGrams})`,
      totalCarbsGrams: sql<number>`sum(${meals.totalCarbsGrams})`,
    })
    .from(meals)
    .where(sql`${day} >= ${startDateInclusive} AND ${day} < ${endDateExclusive}`)
    .groupBy(day)
    .orderBy(day)
    .all();
}

/** Every meal marked as favorite, whatever day it belongs to, ordered by when it occurred (newest first). */
export function listFavoriteMeals(db: AppDatabase): MealEntry[] {
  const mealRows = db.select().from(meals).where(eq(meals.isFavorite, true)).orderBy(desc(meals.occurredAt)).all();

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
      mealType: input.mealType !== undefined ? input.mealType : existing.mealType,
      isFavorite: input.isFavorite !== undefined ? input.isFavorite : existing.isFavorite,
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
