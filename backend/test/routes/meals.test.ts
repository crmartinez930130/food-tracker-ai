import { describe, expect, it } from "vitest";
import request from "supertest";
import sharp from "sharp";
import { buildTestApp } from "../helpers/test-app.js";

async function sampleJpeg(): Promise<Buffer> {
  return sharp({
    create: { width: 100, height: 100, channels: 3, background: { r: 180, g: 140, b: 90 } },
  })
    .jpeg()
    .toBuffer();
}

const sampleItem = {
  name: "Grilled chicken breast",
  estimatedGrams: 150,
  confidence: "high" as const,
  calories: 248,
  proteinGrams: 46.5,
  fatGrams: 5.4,
  carbsGrams: 0,
};

describe("POST /api/meals/analyze", () => {
  it("returns a structured analysis for a valid JPEG without persisting a meal", async () => {
    const parsedAnalysis = {
      items: [sampleItem],
      totalCalories: 248,
      totalProteinGrams: 46.5,
      totalFatGrams: 5.4,
      totalCarbsGrams: 0,
    };
    const { app, db } = buildTestApp(async () => ({
      parsed_output: parsedAnalysis,
      usage: { input_tokens: 100, output_tokens: 50 },
    }));

    const jpeg = await sampleJpeg();
    const res = await request(app).post("/api/meals/analyze").attach("photo", jpeg, {
      filename: "meal.jpg",
      contentType: "image/jpeg",
    });

    expect(res.status).toBe(200);
    expect(res.body).toEqual(parsedAnalysis);

    const { listMealsByDate } = await import("../../src/db/meal-repository.js");
    const today = new Date().toISOString().slice(0, 10);
    expect(listMealsByDate(db, today)).toEqual([]);
  });

  it("rejects an unsupported file type without calling the vision service", async () => {
    let called = false;
    const { app } = buildTestApp(async () => {
      called = true;
      return { parsed_output: null, usage: { input_tokens: 0, output_tokens: 0 } };
    });

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", Buffer.from("not an image"), { filename: "notes.txt", contentType: "text/plain" });

    expect(res.status).toBe(400);
    expect(called).toBe(false);
  });

  it("returns an empty-items analysis rather than an error when no food is detected", async () => {
    const { app } = buildTestApp(async () => ({
      parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
      usage: { input_tokens: 80, output_tokens: 10 },
    }));

    const jpeg = await sampleJpeg();
    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", jpeg, { filename: "empty-plate.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(200);
    expect(res.body.items).toEqual([]);
  });
});

describe("POST /api/meals", () => {
  it("saves the corrected values when the client edits the analysis before confirming", async () => {
    const { app } = buildTestApp();
    const corrected = { ...sampleItem, estimatedGrams: 200, calories: 330 };

    const res = await request(app).post("/api/meals").send({ items: [corrected] });

    expect(res.status).toBe(201);
    expect(res.body.items[0].estimatedGrams).toBe(200);
    expect(res.body.totalCalories).toBe(330);
  });

  it("accepts an explicit JSON null for occurredAt/notes, not just an omitted key", async () => {
    // kotlinx.serialization (and other typed clients) send `"occurredAt": null`
    // rather than omitting the key - the schema must accept null, not just undefined.
    const { app } = buildTestApp();
    const res = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt: null, notes: null });

    expect(res.status).toBe(201);
    expect(res.body.notes).toBeNull();
  });

  it("persists the mealType when provided", async () => {
    const { app } = buildTestApp();
    const res = await request(app).post("/api/meals").send({ items: [sampleItem], mealType: "breakfast" });

    expect(res.status).toBe(201);
    expect(res.body.mealType).toBe("breakfast");
  });

  it("defaults mealType to null when omitted", async () => {
    const { app } = buildTestApp();
    const res = await request(app).post("/api/meals").send({ items: [sampleItem] });

    expect(res.status).toBe(201);
    expect(res.body.mealType).toBeNull();
  });
});

describe("GET /api/meals", () => {
  it("returns an empty list for a date with no saved meals", async () => {
    const { app } = buildTestApp();
    const res = await request(app).get("/api/meals").query({ date: "2026-01-01" });
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it("includes mealType in every returned entry", async () => {
    const { app } = buildTestApp();
    await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], mealType: "lunch", occurredAt: "2026-01-10T12:00:00.000Z" });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-01-10T18:00:00.000Z" });

    const res = await request(app).get("/api/meals").query({ date: "2026-01-10" });
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(2);
    expect(res.body.every((entry: { mealType: unknown }) => "mealType" in entry)).toBe(true);
    expect(res.body.map((entry: { mealType: string | null }) => entry.mealType).sort()).toEqual(["lunch", null]);
  });
});

describe("GET /api/meals/:id", () => {
  it("returns 404 for an id that does not exist", async () => {
    const { app } = buildTestApp();
    const res = await request(app).get("/api/meals/999");
    expect(res.status).toBe(404);
  });

  it("includes mealType (null) for an entry saved without one", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/meals").send({ items: [sampleItem] });

    const fetched = await request(app).get(`/api/meals/${created.body.id}`);
    expect(fetched.body.mealType).toBeNull();
  });
});

describe("PATCH /api/meals/:id", () => {
  it("reflects the edit on subsequent reads", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/meals").send({ items: [sampleItem] });

    const patched = await request(app)
      .patch(`/api/meals/${created.body.id}`)
      .send({ notes: "extra rice" });
    expect(patched.status).toBe(200);
    expect(patched.body.notes).toBe("extra rice");

    const fetched = await request(app).get(`/api/meals/${created.body.id}`);
    expect(fetched.body.notes).toBe("extra rice");
  });

  it("updates the mealType", async () => {
    const { app } = buildTestApp();
    const created = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], mealType: "breakfast" });

    const patched = await request(app).patch(`/api/meals/${created.body.id}`).send({ mealType: "snack" });
    expect(patched.status).toBe(200);
    expect(patched.body.mealType).toBe("snack");
  });
});

describe("DELETE /api/meals/:id", () => {
  it("removes the entry so it no longer appears in date listings", async () => {
    const { app } = buildTestApp();
    const created = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt: "2026-01-05T12:00:00.000Z" });

    const del = await request(app).delete(`/api/meals/${created.body.id}`);
    expect(del.status).toBe(204);

    const listed = await request(app).get("/api/meals").query({ date: "2026-01-05" });
    expect(listed.body).toEqual([]);
  });
});

describe("favorites", () => {
  it("includes isFavorite (false) on newly saved meals", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/meals").send({ items: [sampleItem] });

    expect(res.body.isFavorite).toBe(false);
  });

  it("marks and unmarks a meal as favorite via PATCH", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/meals").send({ items: [sampleItem] });

    const marked = await request(app).patch(`/api/meals/${created.body.id}`).send({ isFavorite: true });
    expect(marked.status).toBe(200);
    expect(marked.body.isFavorite).toBe(true);

    const unmarked = await request(app).patch(`/api/meals/${created.body.id}`).send({ isFavorite: false });
    expect(unmarked.body.isFavorite).toBe(false);
    expect((await request(app).get(`/api/meals/${created.body.id}`)).body.isFavorite).toBe(false);
  });

  it("GET /api/meals/favorites returns only favorites, from any date, newest first", async () => {
    const { app } = buildTestApp();
    const older = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt: "2026-01-01T12:00:00.000Z" });
    const newer = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt: "2026-03-15T12:00:00.000Z" });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-02-10T12:00:00.000Z" });
    await request(app).patch(`/api/meals/${older.body.id}`).send({ isFavorite: true });
    await request(app).patch(`/api/meals/${newer.body.id}`).send({ isFavorite: true });

    const res = await request(app).get("/api/meals/favorites");

    expect(res.status).toBe(200);
    expect(res.body.map((m: { id: number }) => m.id)).toEqual([newer.body.id, older.body.id]);
    expect(res.body[0].items[0].name).toBe(sampleItem.name);
  });

  it("GET /api/meals/favorites returns an empty list when there are none (not routed to /:id)", async () => {
    const { app } = buildTestApp();
    await request(app).post("/api/meals").send({ items: [sampleItem] });

    const res = await request(app).get("/api/meals/favorites");

    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it("rejects a non-boolean isFavorite with 400 and leaves the meal unchanged", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/meals").send({ items: [sampleItem] });

    const res = await request(app).patch(`/api/meals/${created.body.id}`).send({ isFavorite: "yes" });

    expect(res.status).toBe(400);
    expect((await request(app).get(`/api/meals/${created.body.id}`)).body.isFavorite).toBe(false);
  });
});

describe("readable validation errors", () => {
  it("POST with malformed items returns 400 with field: message and no raw JSON", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/meals").send({ items: [{ name: "x", calories: "lots" }] });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^items\.0\.\w+: /);
    expect(res.body.error).not.toContain("{");
  });

  it("POST with a non-object body names the body", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/meals").set("content-type", "application/json").send("[1,2]");

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^body: /);
  });

  it("PATCH with a wrong-typed field names it and leaves the meal unchanged", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/meals").send({ items: [sampleItem], notes: "keep" });

    const res = await request(app).patch(`/api/meals/${created.body.id}`).send({ isFavorite: "yes" });
    const after = await request(app).get(`/api/meals/${created.body.id}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^isFavorite: /);
    expect(after.body.isFavorite).toBe(false);
    expect(after.body.notes).toBe("keep");
  });
});

describe("GET /api/meals date validation", () => {
  it("rejects an impossible date with 400", async () => {
    const { app } = buildTestApp();

    expect((await request(app).get("/api/meals").query({ date: "2026-02-30" })).status).toBe(400);
  });

  it("still accepts a real date", async () => {
    const { app } = buildTestApp();

    expect((await request(app).get("/api/meals").query({ date: "2026-02-28" })).status).toBe(200);
  });
});

describe("GET /api/meals/export", () => {
  const HEADER = "date,time,meal_type,item,grams,calories,protein_g,fat_g,carbs_g,notes";
  // Local wall-clock 13:15, so the expected local date/time hold in any test timezone.
  const localNoon = (date: string) => new Date(`${date}T13:15:00`).toISOString();

  it("returns the range's meals as a CSV attachment, one row per item", async () => {
    const { app } = buildTestApp();
    await request(app)
      .post("/api/meals")
      .send({
        items: [sampleItem, { ...sampleItem, name: "Ñoquis con jamón", calories: 300 }],
        occurredAt: localNoon("2026-10-02"),
        mealType: "lunch",
        notes: 'with "salsa", extra',
      });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: localNoon("2026-10-09") });

    const res = await request(app).get("/api/meals/export").query({ startDate: "2026-10-01", endDate: "2026-10-07" });

    expect(res.status).toBe(200);
    expect(res.headers["content-type"]).toBe("text/csv; charset=utf-8");
    expect(res.headers["content-disposition"]).toBe('attachment; filename="food-tracker-2026-10-01_2026-10-07.csv"');
    expect(res.text).toBe(
      `﻿${HEADER}\r\n` +
        '2026-10-02,13:15,lunch,Grilled chicken breast,150,248,46.5,5.4,0,"with ""salsa"", extra"\r\n' +
        '2026-10-02,13:15,lunch,Ñoquis con jamón,150,300,46.5,5.4,0,"with ""salsa"", extra"\r\n',
    );
  });

  it("returns only the header for a range without meals", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/meals/export").query({ startDate: "2026-10-01", endDate: "2026-10-01" });

    expect(res.status).toBe(200);
    expect(res.text).toBe(`﻿${HEADER}\r\n`);
  });

  it.each([
    ["a missing date", { startDate: "2026-10-01" }],
    ["an impossible date", { startDate: "2026-02-30", endDate: "2026-03-01" }],
    ["an end before the start", { startDate: "2026-10-07", endDate: "2026-10-01" }],
    ["a range over 366 days", { startDate: "2025-01-01", endDate: "2026-01-02" }],
  ])("rejects %s with 400", async (_label, query) => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/meals/export").query(query);

    expect(res.status).toBe(400);
    expect(res.body.error).toBeTypeOf("string");
  });

  it("accepts exactly 366 days", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/meals/export").query({ startDate: "2025-01-01", endDate: "2026-01-01" });

    expect(res.status).toBe(200);
  });
});
