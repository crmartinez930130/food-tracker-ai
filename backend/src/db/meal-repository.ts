import { asc, desc, eq, sql } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { meals, mealItems, type NewMealItemRow } from "./schema.js";
import {
  computeTotals,
  type ExportRow,
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
    clientId: mealRow.clientId,
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

/** A save's result: the meal, and whether this call created it or it was already stored. */
export interface SaveMealResult {
  entry: MealEntry;
  created: boolean;
}

/**
 * Saves a meal at most once per `clientId`: when a meal with it is already stored, that meal is
 * returned and nothing is written. The lookup and the inserts run in one transaction, so a retry
 * can't slip in between them and the meal never ends up stored without its items. Reads go
 * through `db`: better-sqlite3 has one connection, so they see the transaction's own writes.
 */
export function saveMeal(db: AppDatabase, input: SaveMealInput): SaveMealResult {
  return db.transaction((tx) => {
    if (input.clientId) {
      const existing = tx.select({ id: meals.id }).from(meals).where(eq(meals.clientId, input.clientId)).get();
      if (existing) return { entry: getMealById(db, existing.id)!, created: false };
    }

    const now = new Date().toISOString();
    const [mealRow] = tx
      .insert(meals)
      .values({
        occurredAt: input.occurredAt ?? now,
        createdAt: now,
        notes: input.notes ?? null,
        mealType: input.mealType ?? null,
        clientId: input.clientId ?? null,
        ...computeTotals(input.items),
      })
      .returning()
      .all();

    if (input.items.length > 0) {
      tx.insert(mealItems).values(itemRowsFor(mealRow.id, input.items)).run();
    }

    return { entry: getMealById(db, mealRow.id)!, created: true };
  });
}

export function createMeal(db: AppDatabase, input: SaveMealInput): MealEntry {
  return saveMeal(db, input).entry;
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
  mealCount: number;
}

/**
 * Summed totals and meal count per local calendar day in [startDateInclusive, endDateExclusive),
 * only for days that have meals, in date order. Aggregates in SQL so items don't need to be loaded.
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
      mealCount: sql<number>`count(*)`,
    })
    .from(meals)
    .where(sql`${day} >= ${startDateInclusive} AND ${day} < ${endDateExclusive}`)
    .groupBy(day)
    .orderBy(day)
    .all();
}

/**
 * The CSV export's rows for meals whose local day is in [startDateInclusive, endDateExclusive):
 * one per item, in chronological order. The LEFT JOIN gives an item-less meal a single row with
 * null item columns, so it isn't dropped. Local date and time come from SQLite's 'localtime', the
 * same conversion that decides which day a meal is listed under.
 */
export function exportRowsBetween(db: AppDatabase, startDateInclusive: string, endDateExclusive: string): ExportRow[] {
  const day = sql<string>`date(${meals.occurredAt}, 'localtime')`;
  const rows = db
    .select({
      date: day,
      time: sql<string>`strftime('%H:%M', ${meals.occurredAt}, 'localtime')`,
      mealType: meals.mealType,
      notes: meals.notes,
      itemId: mealItems.id,
      name: mealItems.name,
      grams: mealItems.estimatedGrams,
      calories: mealItems.calories,
      proteinGrams: mealItems.proteinGrams,
      fatGrams: mealItems.fatGrams,
      carbsGrams: mealItems.carbsGrams,
    })
    .from(meals)
    .leftJoin(mealItems, eq(mealItems.mealId, meals.id))
    .where(sql`${day} >= ${startDateInclusive} AND ${day} < ${endDateExclusive}`)
    .orderBy(asc(meals.occurredAt), asc(meals.id), asc(mealItems.id))
    .all();

  return rows.map((row) => ({
    date: row.date,
    time: row.time,
    mealType: row.mealType,
    notes: row.notes,
    item:
      row.itemId === null
        ? null
        : {
            name: row.name!,
            grams: row.grams!,
            calories: row.calories!,
            proteinGrams: row.proteinGrams!,
            fatGrams: row.fatGrams!,
            carbsGrams: row.carbsGrams!,
          },
  }));
}

/** The local calendar day (YYYY-MM-DD) of the earliest meal, or null when there are none. */
export function firstMealDate(db: AppDatabase): string | null {
  const row = db
    .select({ date: sql<string | null>`date(min(${meals.occurredAt}), 'localtime')` })
    .from(meals)
    .get();
  return row?.date ?? null;
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
