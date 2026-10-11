import { ValidationError } from "../errors.js";

export const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
export const MAX_RANGE_DAYS = 366;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** YYYY-MM-DD that is also a real calendar date (the regex alone accepts 2026-02-30). */
export function isCalendarDate(value: unknown): value is string {
  if (typeof value !== "string" || !DATE_RE.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

/**
 * Validates an inclusive `startDate`/`endDate` query pair: real dates, end not before start, at
 * most MAX_RANGE_DAYS days. Throws ValidationError otherwise.
 */
export function parseDateRange(query: { startDate?: unknown; endDate?: unknown }): {
  startDate: string;
  endDate: string;
} {
  const { startDate, endDate } = query;
  if (!isCalendarDate(startDate) || !isCalendarDate(endDate)) {
    throw new ValidationError("Query parameters 'startDate' and 'endDate' must be dates in YYYY-MM-DD format");
  }
  const days = (Date.parse(endDate) - Date.parse(startDate)) / MS_PER_DAY + 1;
  if (days < 1) {
    throw new ValidationError("'endDate' must not be before 'startDate'");
  }
  if (days > MAX_RANGE_DAYS) {
    throw new ValidationError(`The range can't be longer than ${MAX_RANGE_DAYS} days`);
  }
  return { startDate, endDate };
}

/**
 * Today's date on the server's clock, as YYYY-MM-DD. Single-user, single-machine app: the
 * server's timezone is the user's (the same assumption as meals' 'localtime' grouping).
 */
export function localToday(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}
