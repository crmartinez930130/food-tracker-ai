import { asc, eq, sql } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { waterLogs, type WaterLogRow } from "./schema.js";

export type WaterLog = WaterLogRow;

export interface WaterDay {
  date: string;
  totalMl: number;
  entries: WaterLog[];
}

/** A log's result: the entry, and whether this call created it or it was already stored. */
export interface LogWaterResult {
  entry: WaterLog;
  created: boolean;
}

/**
 * Logs water at most once per `clientId` (lower case): when an entry with it is already stored,
 * that entry is returned and nothing is written. Lookup and insert run in one transaction.
 */
export function logWater(db: AppDatabase, amountMl: number, loggedAt: string, clientId?: string | null): LogWaterResult {
  return db.transaction((tx) => {
    if (clientId) {
      const existing = tx.select().from(waterLogs).where(eq(waterLogs.clientId, clientId)).get();
      if (existing) return { entry: existing, created: false };
    }
    const entry = tx.insert(waterLogs).values({ amountMl, loggedAt, clientId: clientId ?? null }).returning().get();
    return { entry, created: true };
  });
}

export function createWaterLog(db: AppDatabase, amountMl: number, loggedAt: string): WaterLog {
  return logWater(db, amountMl, loggedAt).entry;
}

/**
 * The water logged on a local calendar day, oldest first, with its total. Like meals, an entry's
 * day is `date(logged_at, 'localtime')`: the server's timezone is the user's (see meal-repository).
 */
export function waterForDay(db: AppDatabase, date: string): WaterDay {
  const entries = db
    .select()
    .from(waterLogs)
    .where(sql`date(${waterLogs.loggedAt}, 'localtime') = ${date}`)
    .orderBy(asc(waterLogs.loggedAt), asc(waterLogs.id))
    .all();
  return { date, totalMl: entries.reduce((sum, entry) => sum + entry.amountMl, 0), entries };
}

export interface WaterDayTotal {
  date: string;
  totalMl: number;
}

/**
 * The water total per local calendar day in [startDateInclusive, endDateExclusive), only for days
 * with entries, in date order - the same day rule as [waterForDay].
 */
export function waterTotalsBetween(db: AppDatabase, startDateInclusive: string, endDateExclusive: string): WaterDayTotal[] {
  const day = sql<string>`date(${waterLogs.loggedAt}, 'localtime')`;
  return db
    .select({ date: day, totalMl: sql<number>`sum(${waterLogs.amountMl})` })
    .from(waterLogs)
    .where(sql`${day} >= ${startDateInclusive} AND ${day} < ${endDateExclusive}`)
    .groupBy(day)
    .orderBy(day)
    .all();
}

export function deleteWaterLog(db: AppDatabase, id: number): boolean {
  return db.delete(waterLogs).where(eq(waterLogs.id, id)).run().changes > 0;
}
