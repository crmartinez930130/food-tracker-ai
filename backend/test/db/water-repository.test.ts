import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { createWaterLog, deleteWaterLog, waterForDay, waterTotalsBetween } from "../../src/db/water-repository.js";

// Built from local wall-clock values, so the expected local day holds in any test timezone.
const local = (d: number, h: number, min = 0) => new Date(2026, 2, d, h, min).toISOString();

let db: AppDatabase;

beforeEach(() => {
  db = openDatabase(":memory:");
});

describe("water repository", () => {
  it("totals a day's entries and lists them oldest first", () => {
    const late = createWaterLog(db, 500, local(5, 18));
    const early = createWaterLog(db, 250, local(5, 8));
    const middle = createWaterLog(db, 330, local(5, 12));

    const day = waterForDay(db, "2026-03-05");

    expect(day.date).toBe("2026-03-05");
    expect(day.totalMl).toBe(1080);
    expect(day.entries.map((e) => e.id)).toEqual([early.id, middle.id, late.id]);
    expect(day.entries[0]).toEqual({ id: early.id, amountMl: 250, loggedAt: local(5, 8) });
  });

  it("returns a total of 0 and no entries for a day without water", () => {
    createWaterLog(db, 250, local(4, 12));

    expect(waterForDay(db, "2026-03-05")).toEqual({ date: "2026-03-05", totalMl: 0, entries: [] });
  });

  it("puts a late-evening entry on its local day", () => {
    createWaterLog(db, 250, local(5, 23, 30));

    expect(waterForDay(db, "2026-03-05").totalMl).toBe(250);
    expect(waterForDay(db, "2026-03-06").totalMl).toBe(0);
  });

  it("deletes an entry, which then no longer counts", () => {
    const keep = createWaterLog(db, 250, local(5, 8));
    const mistake = createWaterLog(db, 250, local(5, 9));

    expect(deleteWaterLog(db, mistake.id)).toBe(true);
    expect(waterForDay(db, "2026-03-05")).toMatchObject({ totalMl: 250, entries: [{ id: keep.id }] });
    expect(deleteWaterLog(db, mistake.id)).toBe(false);
  });
});

describe("waterTotalsBetween", () => {
  it("sums each local day, omits days without water, and orders by day", () => {
    createWaterLog(db, 500, local(3, 9));
    createWaterLog(db, 250, local(1, 8));
    createWaterLog(db, 250, local(1, 23, 30)); // still the 1st, locally
    createWaterLog(db, 250, local(10, 12)); // outside the range

    expect(waterTotalsBetween(db, "2026-03-01", "2026-03-05")).toEqual([
      { date: "2026-03-01", totalMl: 500 },
      { date: "2026-03-03", totalMl: 500 },
    ]);
  });
});
