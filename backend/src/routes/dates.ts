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

/** The one wording for a bad date query parameter, whether malformed or impossible (2026-02-30). */
export function invalidDateMessage(name: string): string {
  return `Query parameter '${name}' must be a real date in YYYY-MM-DD format`;
}

export const INVALID_RANGE_MESSAGE = "Query parameters 'startDate' and 'endDate' must be real dates in YYYY-MM-DD format";

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
    throw new ValidationError(INVALID_RANGE_MESSAGE);
  }
  checkRangeLength(startDate, endDate);
  return { startDate, endDate };
}

/**
 * Throws ValidationError unless the inclusive range of two real dates has its end not before its
 * start and spans at most [maxDays] days.
 */
export function checkRangeLength(startDate: string, endDate: string, maxDays: number = MAX_RANGE_DAYS): void {
  const days = (Date.parse(endDate) - Date.parse(startDate)) / MS_PER_DAY + 1;
  if (days < 1) {
    throw new ValidationError("'endDate' must not be before 'startDate'");
  }
  if (days > maxDays) {
    throw new ValidationError(`The range can't be longer than ${maxDays} days`);
  }
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
