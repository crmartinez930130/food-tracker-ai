import { describe, expect, it } from "vitest";
import { CSV_HEADER, exportFileName, toCsv } from "../../src/services/csv-export.js";
import type { ExportRow } from "../../src/domain/meal.js";

/** Minimal RFC 4180 reader, to check the output parses back to the original values. */
function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') {
        field += '"';
        i++;
      } else if (c === '"') {
        quoted = false;
      } else {
        field += c;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ",") {
      row.push(field);
      field = "";
    } else if (c === "\r" && text[i + 1] === "\n") {
      row.push(field);
      rows.push(row);
      row = [];
      field = "";
      i++;
    } else {
      field += c;
    }
  }
  if (field !== "" || row.length > 0) throw new Error("CSV does not end with CRLF");
  return rows;
}

const item = { name: "Rice", grams: 151.6, calories: 199.5, proteinGrams: 4.04, fatGrams: 0.46, carbsGrams: 44.25 };
const row = (overrides: Partial<ExportRow> = {}): ExportRow => ({
  date: "2026-03-05",
  time: "13:15",
  mealType: "lunch",
  notes: null,
  item,
  ...overrides,
});
const body = (csv: string) => parseCsv(csv.slice(1)).slice(1);

describe("toCsv", () => {
  it("starts with a UTF-8 BOM and the header row, and ends every line with CRLF", () => {
    const csv = toCsv([row()]);
    expect(csv.startsWith("﻿date,time,meal_type,item,grams,calories,protein_g,fat_g,carbs_g,notes\r\n")).toBe(true);
    expect(csv.endsWith("\r\n")).toBe(true);
    expect(csv.split("\r\n")).toHaveLength(3);
  });

  it("returns only the header for no rows", () => {
    expect(toCsv([])).toBe(`﻿${CSV_HEADER.join(",")}\r\n`);
  });

  it("writes whole grams and calories and one-decimal macros with a dot", () => {
    expect(body(toCsv([row()]))).toEqual([["2026-03-05", "13:15", "lunch", "Rice", "152", "200", "4", "0.5", "44.3", ""]]);
  });

  it("leaves the item columns empty for a meal without items and the type empty when unset", () => {
    expect(body(toCsv([row({ item: null, mealType: null, notes: "skipped photo" })]))).toEqual([
      ["2026-03-05", "13:15", "", "", "", "", "", "", "", "skipped photo"],
    ]);
  });

  it("quotes commas, quotes and line breaks so they parse back unchanged", () => {
    const notes = 'with "salsa", extra\nand a second line\r\nthird';
    const csv = toCsv([row({ notes, item: { ...item, name: "Tacos, al pastor" } })]);
    expect(csv).toContain('"with ""salsa"", extra');
    const [parsed] = body(csv);
    expect(parsed).toHaveLength(CSV_HEADER.length);
    expect(parsed[3]).toBe("Tacos, al pastor");
    expect(parsed[9]).toBe(notes);
  });

  it("keeps accented names unchanged", () => {
    expect(body(toCsv([row({ item: { ...item, name: "Ñoquis con jamón" } })]))[0][3]).toBe("Ñoquis con jamón");
  });

  it.each(["=HYPERLINK(\"x\")", "+1", "-2", "@SUM(A1)", "\tcell", "\rcell"])(
    "prefixes text starting like a formula (%j) with a quote",
    (text) => {
      const [parsed] = body(toCsv([row({ notes: text, item: { ...item, name: text } })]));
      expect(parsed[3]).toBe(`'${text}`);
      expect(parsed[9]).toBe(`'${text}`);
    },
  );

  it("keeps rows in the given order", () => {
    const rows = [row({ time: "08:00" }), row({ time: "13:00" }), row({ time: "20:00" })];
    expect(body(toCsv(rows)).map((r) => r[1])).toEqual(["08:00", "13:00", "20:00"]);
  });
});

describe("exportFileName", () => {
  it("names the file after the range", () => {
    expect(exportFileName("2026-10-01", "2026-10-07")).toBe("food-tracker-2026-10-01_2026-10-07.csv");
  });
});
