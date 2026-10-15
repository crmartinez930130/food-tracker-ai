import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { createWaterLog, deleteWaterLog, logWater, waterForDay, waterTotalsBetween } from "../../src/db/water-repository.js";

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
    expect(day.entries[0]).toEqual({ id: early.id, amountMl: 250, loggedAt: local(5, 8), clientId: null });
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

  describe("logWater with a clientId", () => {
    const clientId = "0b6f1c3e-5d2a-4f8b-9c7e-1a2b3c4d5e6f";

    it("creates the entry the first time", () => {
      const { entry, created } = logWater(db, 250, local(5, 12), clientId);

      expect(created).toBe(true);
      expect(entry).toMatchObject({ amountMl: 250, clientId });
    });

    it("returns the stored entry on a retry, without adding to the day's total", () => {
      const first = logWater(db, 250, local(5, 12), clientId);

      const retry = logWater(db, 500, local(5, 13), clientId);

      expect(retry).toEqual({ entry: first.entry, created: false });
      expect(waterForDay(db, "2026-03-05").totalMl).toBe(250);
    });

    it("always creates an entry logged without a clientId", () => {
      logWater(db, 250, local(5, 12));
      logWater(db, 250, local(5, 12), null);

      expect(waterForDay(db, "2026-03-05").totalMl).toBe(500);
    });
  });
});
