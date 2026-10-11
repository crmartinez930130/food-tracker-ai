import { and, asc, eq, gte, lte } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { weightLogs, type WeightLogRow } from "./schema.js";

export type WeightLog = WeightLogRow;

export interface WeightLogInput {
  weightKg: number;
  /** The user's local calendar day, YYYY-MM-DD. */
  day: string;
}

export function createWeightLog(
  db: AppDatabase,
  input: WeightLogInput,
  createdAt: string = new Date().toISOString(),
): WeightLog {
  return db.insert(weightLogs).values({ weightKg: input.weightKg, day: input.day, createdAt }).returning().get();
}

/** Entries whose day is within [startDay, endDay] (inclusive), oldest first. */
export function listWeightLogs(db: AppDatabase, startDay: string, endDay: string): WeightLog[] {
  return db
    .select()
    .from(weightLogs)
    .where(and(gte(weightLogs.day, startDay), lte(weightLogs.day, endDay)))
    .orderBy(asc(weightLogs.day), asc(weightLogs.createdAt), asc(weightLogs.id))
    .all();
}

export function deleteWeightLog(db: AppDatabase, id: number): boolean {
  return db.delete(weightLogs).where(eq(weightLogs.id, id)).run().changes > 0;
}
