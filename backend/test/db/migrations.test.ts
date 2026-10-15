import { cpSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import Database from "better-sqlite3";
import { drizzle } from "drizzle-orm/better-sqlite3";
import { migrate } from "drizzle-orm/better-sqlite3/migrator";
import { describe, expect, it } from "vitest";
import { openDatabase } from "../../src/db/client.js";
import { getMealById } from "../../src/db/meal-repository.js";
import { waterForDay } from "../../src/db/water-repository.js";

const MIGRATIONS = resolve(import.meta.dirname, "../../src/db/migrations");

/** A copy of the migrations folder that stops at (and includes) migration `lastIdx`. */
function migrationsUpTo(dir: string, lastIdx: number): string {
  const folder = join(dir, "migrations");
  cpSync(MIGRATIONS, folder, { recursive: true });
  const journalPath = join(folder, "meta", "_journal.json");
  const journal = JSON.parse(readFileSync(journalPath, "utf8"));
  journal.entries = journal.entries.filter((entry: { idx: number }) => entry.idx <= lastIdx);
  writeFileSync(journalPath, JSON.stringify(journal));
  return folder;
}

describe("migration 0006 (client ids)", () => {
  it("keeps meals and water stored before it, with a null clientId", () => {
    const dir = mkdtempSync(join(tmpdir(), "migrations-test-"));
    try {
      const dbPath = join(dir, "food-tracker.sqlite");
      const before = new Database(dbPath);
      migrate(drizzle(before), { migrationsFolder: migrationsUpTo(dir, 5) });
      before
        .prepare(
          `INSERT INTO meals (id, occurred_at, created_at, total_calories, total_protein_grams, total_fat_grams, total_carbs_grams, notes)
           VALUES (1, '2026-03-05T12:00:00.000Z', '2026-03-05T12:00:00.000Z', 300, 20, 10, 30, 'Lunch')`,
        )
        .run();
      before
        .prepare(
          `INSERT INTO meal_items (meal_id, name, estimated_grams, confidence, calories, protein_grams, fat_grams, carbs_grams)
           VALUES (1, 'Rice', 150, 'high', 300, 20, 10, 30)`,
        )
        .run();
      before.prepare(`INSERT INTO water_logs (amount_ml, logged_at) VALUES (250, '2026-03-05T12:00:00.000Z')`).run();
      before.close();

      const db = openDatabase(dbPath);

      expect(getMealById(db, 1)).toMatchObject({ notes: "Lunch", clientId: null, items: [{ name: "Rice" }] });
      const day = new Date("2026-03-05T12:00:00.000Z");
      const localDay = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, "0")}-${String(day.getDate()).padStart(2, "0")}`;
      expect(waterForDay(db, localDay).entries).toEqual([
        { id: 1, amountMl: 250, loggedAt: "2026-03-05T12:00:00.000Z", clientId: null },
      ]);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });
});
