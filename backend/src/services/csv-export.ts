import type { AppDatabase } from "../db/client.js";
import { exportRowsBetween } from "../db/meal-repository.js";
import type { ExportRow } from "../domain/meal.js";
import { addDaysToDateString } from "./summary.js";

export const CSV_HEADER = ["date", "time", "meal_type", "item", "grams", "calories", "protein_g", "fat_g", "carbs_g", "notes"];

// Excel opens BOM-less UTF-8 as the system ANSI codepage and mangles accents.
const BOM = "﻿";
const EOL = "\r\n";
// Cells a spreadsheet would evaluate as a formula (OWASP "CSV injection").
const FORMULA_START = /^[=+\-@\t\r]/;

/** RFC 4180: quote a field holding a comma, quote or line break, doubling inner quotes. */
function escapeField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Free text (item names come from the model, notes from the user) must never run as a formula. */
function textCell(value: string | null): string {
  if (value === null) return "";
  return FORMULA_START.test(value) ? `'${value}` : value;
}

const whole = (n: number) => String(Math.round(n));
const oneDecimal = (n: number) => String(Math.round(n * 10) / 10);

export function toCsv(rows: ExportRow[]): string {
  const lines = rows.map((row) => {
    const { item } = row;
    return [
      row.date,
      row.time,
      row.mealType ?? "",
      textCell(item?.name ?? null),
      item ? whole(item.grams) : "",
      item ? whole(item.calories) : "",
      item ? oneDecimal(item.proteinGrams) : "",
      item ? oneDecimal(item.fatGrams) : "",
      item ? oneDecimal(item.carbsGrams) : "",
      textCell(row.notes),
    ]
      .map(escapeField)
      .join(",");
  });
  return BOM + [CSV_HEADER.join(","), ...lines].map((line) => line + EOL).join("");
}

/** Same pattern the app uses for the shared file. */
export function exportFileName(startDate: string, endDate: string): string {
  return `food-tracker-${startDate}_${endDate}.csv`;
}

/** The CSV for meals whose local day is in the inclusive range [startDate, endDate]. */
export function exportMealsCsv(db: AppDatabase, startDate: string, endDate: string): string {
  return toCsv(exportRowsBetween(db, startDate, addDaysToDateString(endDate, 1)));
}
