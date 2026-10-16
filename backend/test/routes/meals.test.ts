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

describe("POST /api/meals/analyze ingredient photos", () => {
  type Content = { type: string; text?: string }[];
  const emptyAnalysis = {
    parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
    usage: { input_tokens: 1, output_tokens: 1 },
  };

  function appRecording() {
    const calls: Content[] = [];
    const { app } = buildTestApp(async (request: unknown) => {
      calls.push((request as { messages: { content: Content }[] }).messages[0].content);
      return emptyAnalysis;
    });
    return { app, calls };
  }

  it("sends the meal photo and each ingredient photo, labelled, before the prompt", async () => {
    const { app, calls } = appRecording();
    const jpeg = await sampleJpeg();

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", jpeg, { filename: "meal.jpg", contentType: "image/jpeg" })
      .attach("ingredients", jpeg, { filename: "a.jpg", contentType: "image/jpeg" })
      .attach("ingredients", jpeg, { filename: "b.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(200);
    const content = calls[0];
    expect(content.filter((part) => part.type === "image")).toHaveLength(3);
    expect(content.filter((part) => part.type === "text").map((part) => part.text)).toEqual([
      "Photo of the meal:",
      "Photo of an ingredient used to prepare it (1 of 2):",
      "Photo of an ingredient used to prepare it (2 of 2):",
      expect.stringContaining("ingredients used to prepare the meal"),
    ]);
  });

  it("analyzes ingredient photos and a description without a meal photo", async () => {
    const { app, calls } = appRecording();

    const res = await request(app)
      .post("/api/meals/analyze")
      .field("description", "yogur con granola")
      .attach("ingredients", await sampleJpeg(), { filename: "label.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(200);
    const prompt = calls[0].filter((part) => part.type === "text").at(-1)!.text!;
    expect(prompt).toContain("From the user's description");
    expect(prompt).toContain("ingredients used to prepare the meal");
  });

  it("rejects a sixth ingredient photo without calling the analysis", async () => {
    const { app, calls } = appRecording();
    const jpeg = await sampleJpeg();
    let req = request(app).post("/api/meals/analyze");
    for (let i = 0; i < 6; i++) req = req.attach("ingredients", jpeg, { filename: `i${i}.jpg`, contentType: "image/jpeg" });

    const res = await req;

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("Too many ingredient photos; the limit is 5");
    expect(calls).toHaveLength(0);
  });

  it("rejects an ingredient file that isn't an image, without calling the analysis", async () => {
    const { app, calls } = appRecording();

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", await sampleJpeg(), { filename: "meal.jpg", contentType: "image/jpeg" })
      .attach("ingredients", Buffer.from("%PDF-1.7 not a photo"), { filename: "label.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("Unsupported image format; only JPEG and PNG are accepted");
    expect(calls).toHaveLength(0);
  });

  it("answers 413 naming the ingredient photo when one is over the size limit", async () => {
    const { app, calls } = appRecording();

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", await sampleJpeg(), { filename: "meal.jpg", contentType: "image/jpeg" })
      .attach("ingredients", Buffer.alloc(16 * 1024 * 1024, 0xff), { filename: "big.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(413);
    expect(res.body.error).toBe("Ingredient photo too large; the limit is 15 MB");
    expect(calls).toHaveLength(0);
  });

  it("rejects photos over 40 MB together, without calling the analysis", async () => {
    const { app, calls } = appRecording();
    const big = Buffer.alloc(14 * 1024 * 1024, 0xff);

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", big, { filename: "meal.jpg", contentType: "image/jpeg" })
      .attach("ingredients", big, { filename: "a.jpg", contentType: "image/jpeg" })
      .attach("ingredients", big, { filename: "b.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("Photos too large together; the limit is 40 MB");
    expect(calls).toHaveLength(0);
  });
});

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

  it("answers 413, not 500, for a photo over the size limit", async () => {
    let called = false;
    const { app } = buildTestApp(async () => {
      called = true;
      return { parsed_output: null, usage: { input_tokens: 0, output_tokens: 0 } };
    });
    const oversized = Buffer.alloc(16 * 1024 * 1024, 0xff);

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", oversized, { filename: "big.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(413);
    expect(res.body.error).toBe("Photo too large; the limit is 15 MB");
    expect(called).toBe(false);
  });

  it("answers 400 for an upload in an unexpected field", async () => {
    const { app } = buildTestApp();

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("picture", Buffer.from([0xff, 0xd8, 0xff]), { filename: "a.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^Invalid upload/);
  });

  it("rejects a file disguised as a JPEG before decoding it", async () => {
    let called = false;
    const { app } = buildTestApp(async () => {
      called = true;
      return { parsed_output: null, usage: { input_tokens: 0, output_tokens: 0 } };
    });
    const svg = Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect width="10" height="10"/></svg>');

    const res = await request(app)
      .post("/api/meals/analyze")
      .attach("photo", svg, { filename: "meal.jpg", contentType: "image/jpeg" });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("Unsupported image format; only JPEG and PNG are accepted");
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

describe("POST /api/meals with a clientId", () => {
  const clientId = "0b6f1c3e-5d2a-4f8b-9c7e-1a2b3c4d5e6f";
  const occurredAt = new Date(2026, 2, 5, 12, 0).toISOString();
  const mealsOnDay = async (app: Parameters<typeof request>[0]) =>
    (await request(app).get("/api/meals").query({ date: "2026-03-05" })).body as unknown[];

  it("creates the meal with 201 and returns its clientId", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId });

    expect(res.status).toBe(201);
    expect(res.body.clientId).toBe(clientId);
  });

  it("answers a retry with 200 and the stored meal, listed only once", async () => {
    const { app } = buildTestApp();
    const first = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId });

    const retry = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId });

    expect(retry.status).toBe(200);
    expect(retry.body).toEqual(first.body);
    expect(await mealsOnDay(app)).toHaveLength(1);
  });

  it("ignores the other fields of a retry", async () => {
    const { app } = buildTestApp();
    const first = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId });

    const retry = await request(app)
      .post("/api/meals")
      .send({ items: [{ ...sampleItem, calories: 999 }], notes: "changed", occurredAt, clientId });

    expect(retry.status).toBe(200);
    expect(retry.body).toEqual(first.body);
  });

  it("treats the same id in upper case as the same meal", async () => {
    const { app } = buildTestApp();
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId });

    const retry = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt, clientId: clientId.toUpperCase() });

    expect(retry.status).toBe(200);
    expect(retry.body.clientId).toBe(clientId);
    expect(await mealsOnDay(app)).toHaveLength(1);
  });

  it("rejects a clientId that isn't a UUID with 400, storing nothing", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId: "meal-1" });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/clientId/);
    expect(await mealsOnDay(app)).toHaveLength(0);
  });

  it("creates two meals for two saves without a clientId, as before", async () => {
    const { app } = buildTestApp();

    const first = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt });
    const second = await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt, clientId: null });

    expect([first.status, second.status]).toEqual([201, 201]);
    expect([first.body.clientId, second.body.clientId]).toEqual([null, null]);
    expect(await mealsOnDay(app)).toHaveLength(2);
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

describe("GET /api/meals/first-date", () => {
  it("returns null when no meal was ever logged", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/meals/first-date");

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ date: null });
  });

  it("returns the local day of the earliest meal (and isn't routed to /:id)", async () => {
    const { app } = buildTestApp();
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: new Date(2026, 2, 9, 12).toISOString() });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: new Date(2025, 10, 2, 12).toISOString() });

    const res = await request(app).get("/api/meals/first-date");

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ date: "2025-11-02" });
  });
});

describe("POST /api/meals/analyze language", () => {
  const emptyAnalysis = {
    parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
    usage: { input_tokens: 1, output_tokens: 1 },
  };

  async function analyzeWith(fields: Record<string, string>): Promise<{ status: number; prompt: string }> {
    let prompt = "";
    const { app } = buildTestApp(async (request: unknown) => {
      const content = (request as { messages: { content: { type: string; text?: string }[] }[] }).messages[0].content;
      prompt = content.filter((part) => part.type === "text").at(-1)!.text!;
      return emptyAnalysis;
    });
    let req = request(app).post("/api/meals/analyze");
    for (const [name, value] of Object.entries(fields)) req = req.field(name, value);
    const res = await req.attach("photo", await sampleJpeg(), { filename: "meal.jpg", contentType: "image/jpeg" });
    return { status: res.status, prompt };
  }

  it("asks for Spanish when the app sends language=es", async () => {
    const { status, prompt } = await analyzeWith({ language: "es" });

    expect(status).toBe(200);
    expect(prompt).toContain("in Spanish (es)");
  });

  it.each([
    ["no language", {}],
    ["an unknown language", { language: "fr" }],
  ])("still analyzes in English with %s", async (_label, fields) => {
    const { status, prompt } = await analyzeWith(fields);

    expect(status).toBe(200);
    expect(prompt).toContain("in English");
  });
});

describe("POST /api/meals/analyze description and portion", () => {
  const emptyAnalysis = {
    parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
    usage: { input_tokens: 1, output_tokens: 1 },
  };
  type Content = { type: string; text?: string }[];

  async function analyze(fields: Record<string, string>, withPhoto: boolean) {
    const calls: Content[] = [];
    const { app } = buildTestApp(async (request: unknown) => {
      calls.push((request as { messages: { content: Content }[] }).messages[0].content);
      return emptyAnalysis;
    });
    let req = request(app).post("/api/meals/analyze");
    for (const [name, value] of Object.entries(fields)) req = req.field(name, value);
    if (withPhoto) req = req.attach("photo", await sampleJpeg(), { filename: "meal.jpg", contentType: "image/jpeg" });
    const res = await req;
    const prompt = calls[0]?.filter((part) => part.type === "text").at(-1)?.text ?? "";
    return { res, calls, prompt, types: calls[0]?.map((part) => part.type) ?? [] };
  }

  it("still analyzes a photo alone, with no context added", async () => {
    const { res, types, prompt } = await analyze({}, true);

    expect(res.status).toBe(200);
    expect(types).toEqual(["text", "image", "text"]);
    expect(prompt).not.toContain("<description>");
  });

  it("passes the description and portion with a photo", async () => {
    const { res, prompt } = await analyze({ description: "  sin aceite  ", portion: "small", language: "es" }, true);

    expect(res.status).toBe(200);
    expect(prompt).toContain("<description>sin aceite</description>");
    expect(prompt).toContain("portion is small");
    expect(prompt).toContain("in Spanish (es)");
  });

  it("analyzes a description without a photo", async () => {
    const { res, types, prompt } = await analyze({ description: "una manzana y un yogur bebible chico" }, false);

    expect(res.status).toBe(200);
    expect(types).toEqual(["text"]);
    expect(prompt).toContain("From the user's description of a meal");
  });

  it.each([
    ["nothing", {}],
    ["a blank description", { description: "   " }],
    ["only a portion", { portion: "small" }],
  ])("rejects %s without a photo, without calling the analysis", async (_label, fields) => {
    const { res, calls } = await analyze(fields, false);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("Provide a photo, ingredient photos, a description, or a mix");
    expect(calls).toHaveLength(0);
  });

  it("rejects a description over 500 characters and accepts exactly 500", async () => {
    const tooLong = await analyze({ description: "a".repeat(501) }, false);
    const atLimit = await analyze({ description: "a".repeat(500) }, false);

    expect(tooLong.res.status).toBe(400);
    expect(tooLong.res.body.error).toBe("description: must be at most 500 characters");
    expect(tooLong.calls).toHaveLength(0);
    expect(atLimit.res.status).toBe(200);
  });

  it("ignores an unknown portion", async () => {
    const { res, prompt } = await analyze({ description: "an apple", portion: "huge" }, false);

    expect(res.status).toBe(200);
    expect(prompt).not.toContain("portion is");
  });
});
