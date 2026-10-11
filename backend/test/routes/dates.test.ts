import { describe, expect, it } from "vitest";
import { localToday } from "../../src/routes/dates.js";

describe("localToday", () => {
  it("formats the given instant as the local YYYY-MM-DD", () => {
    // Built from local components, so the expectation holds in any timezone.
    expect(localToday(new Date(2026, 0, 5, 23, 30))).toBe("2026-01-05");
    expect(localToday(new Date(2026, 11, 31, 0, 1))).toBe("2026-12-31");
  });
});
