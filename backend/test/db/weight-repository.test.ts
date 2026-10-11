import { beforeEach, describe, expect, it } from "vitest";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { createWeightLog, deleteWeightLog, listWeightLogs } from "../../src/db/weight-repository.js";

let db: AppDatabase;

beforeEach(() => {
  db = openDatabase(":memory:");
});

describe("weight repository", () => {
  it("creates an entry and returns it with its id", () => {
    const entry = createWeightLog(db, { weightKg: 72.4, day: "2026-03-02" });

    expect(entry).toMatchObject({ weightKg: 72.4, day: "2026-03-02" });
    expect(entry.id).toBeTypeOf("number");
    expect(entry.createdAt).toMatch(/^\d{4}-\d{2}-\d{2}T/);
  });

  it("lists only entries inside the inclusive range, by day then creation time", () => {
    createWeightLog(db, { weightKg: 73.0, day: "2026-03-03" }, "2026-03-03T07:00:00.000Z");
    createWeightLog(db, { weightKg: 72.0, day: "2026-03-01" }, "2026-03-01T07:00:00.000Z");
    createWeightLog(db, { weightKg: 72.6, day: "2026-03-03" }, "2026-03-03T20:00:00.000Z");
    createWeightLog(db, { weightKg: 71.0, day: "2026-02-28" }, "2026-02-28T07:00:00.000Z"); // before
    createWeightLog(db, { weightKg: 74.0, day: "2026-03-04" }, "2026-03-04T07:00:00.000Z"); // after

    const entries = listWeightLogs(db, "2026-03-01", "2026-03-03");

    expect(entries.map((e) => [e.day, e.weightKg])).toEqual([
      ["2026-03-01", 72.0],
      ["2026-03-03", 73.0],
      ["2026-03-03", 72.6],
    ]);
  });

  it("returns an empty list for a range without entries", () => {
    createWeightLog(db, { weightKg: 72.0, day: "2026-03-01" });

    expect(listWeightLogs(db, "2026-04-01", "2026-04-30")).toEqual([]);
  });

  it("deletes an existing entry and reports a missing one", () => {
    const entry = createWeightLog(db, { weightKg: 72.0, day: "2026-03-01" });

    expect(deleteWeightLog(db, entry.id)).toBe(true);
    expect(listWeightLogs(db, "2026-03-01", "2026-03-01")).toEqual([]);
    expect(deleteWeightLog(db, entry.id)).toBe(false);
  });
});
