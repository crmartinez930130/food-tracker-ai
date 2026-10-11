import type { AppDatabase } from "../db/client.js";
import { dailyTotalsBetween, listMealsBetween, listMealsByDate, type DayTotals } from "../db/meal-repository.js";
import type { MealTotals } from "../domain/meal.js";

const ZERO_TOTALS: MealTotals = {
  totalCalories: 0,
  totalProteinGrams: 0,
  totalFatGrams: 0,
  totalCarbsGrams: 0,
};

function sumTotals(entries: MealTotals[]): MealTotals {
  return entries.reduce<MealTotals>(
    (acc, entry) => ({
      totalCalories: acc.totalCalories + entry.totalCalories,
      totalProteinGrams: acc.totalProteinGrams + entry.totalProteinGrams,
      totalFatGrams: acc.totalFatGrams + entry.totalFatGrams,
      totalCarbsGrams: acc.totalCarbsGrams + entry.totalCarbsGrams,
    }),
    { ...ZERO_TOTALS },
  );
}

export function addDaysToDateString(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function dailySummary(db: AppDatabase, date: string): MealTotals {
  return sumTotals(listMealsByDate(db, date));
}

export interface WeeklySummary {
  total: MealTotals;
  averagePerDay: MealTotals;
}

const DAYS_IN_WEEK = 7;

export function weeklySummary(db: AppDatabase, startDate: string): WeeklySummary {
  const endDateExclusive = addDaysToDateString(startDate, DAYS_IN_WEEK);

  const total = sumTotals(listMealsBetween(db, startDate, endDateExclusive));
  const averagePerDay: MealTotals = {
    totalCalories: total.totalCalories / DAYS_IN_WEEK,
    totalProteinGrams: total.totalProteinGrams / DAYS_IN_WEEK,
    totalFatGrams: total.totalFatGrams / DAYS_IN_WEEK,
    totalCarbsGrams: total.totalCarbsGrams / DAYS_IN_WEEK,
  };

  return { total, averagePerDay };
}

/** One entry per day from startDate to endDate (both inclusive), zero-filled where nothing was logged. */
export function rangeSummary(db: AppDatabase, startDate: string, endDate: string): DayTotals[] {
  const loggedDays = new Map(
    dailyTotalsBetween(db, startDate, addDaysToDateString(endDate, 1)).map((day) => [day.date, day]),
  );

  const days: DayTotals[] = [];
  for (let date = startDate; date <= endDate; date = addDaysToDateString(date, 1)) {
    days.push(loggedDays.get(date) ?? { date, ...ZERO_TOTALS });
  }
  return days;
}
