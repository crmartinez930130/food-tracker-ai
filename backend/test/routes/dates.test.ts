import { describe, expect, it } from "vitest";
import { checkRangeLength, localToday, MAX_RANGE_DAYS } from "../../src/routes/dates.js";
import { ValidationError } from "../../src/errors.js";

describe("localToday", () => {
  it("formats the given instant as the local YYYY-MM-DD", () => {
    // Built from local components, so the expectation holds in any timezone.
    expect(localToday(new Date(2026, 0, 5, 23, 30))).toBe("2026-01-05");
    expect(localToday(new Date(2026, 11, 31, 0, 1))).toBe("2026-12-31");
  });
});

describe("checkRangeLength", () => {
  it("accepts a range up to the given maximum, both ends included", () => {
    expect(() => checkRangeLength("2026-01-01", "2026-01-31", 31)).not.toThrow();
    expect(() => checkRangeLength("2026-01-01", "2026-01-01", 31)).not.toThrow();
  });

  it("rejects a range longer than the given maximum", () => {
    expect(() => checkRangeLength("2026-01-01", "2026-02-01", 31)).toThrow(
      new ValidationError("The range can't be longer than 31 days"),
    );
  });

  it("defaults to the API's maximum", () => {
    expect(() => checkRangeLength("2026-01-01", "2027-01-01")).not.toThrow();
    expect(() => checkRangeLength("2026-01-01", "2027-01-02")).toThrow(`longer than ${MAX_RANGE_DAYS} days`);
  });

  it("rejects an end before the start", () => {
    expect(() => checkRangeLength("2026-01-02", "2026-01-01", 31)).toThrow("'endDate' must not be before 'startDate'");
  });
});
