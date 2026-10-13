import { asc, eq, sql } from "drizzle-orm";
import type { AppDatabase } from "./client.js";
import { waterLogs, type WaterLogRow } from "./schema.js";

export type WaterLog = WaterLogRow;

export interface WaterDay {
  date: string;
  totalMl: number;
  entries: WaterLog[];
}

export function createWaterLog(db: AppDatabase, amountMl: number, loggedAt: string): WaterLog {
  return db.insert(waterLogs).values({ amountMl, loggedAt }).returning().get();
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
