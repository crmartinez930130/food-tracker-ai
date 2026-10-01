import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";

const sampleItem = {
  name: "Grilled chicken breast",
  estimatedGrams: 150,
  confidence: "high" as const,
  calories: 248,
  proteinGrams: 46.5,
  fatGrams: 5.4,
  carbsGrams: 0,
};

describe("GET /api/summary/daily", () => {
  it("returns zeroed totals for a day with no saved meals", async () => {
    const { app } = buildTestApp();
    const res = await request(app).get("/api/summary/daily").query({ date: "2026-02-01" });
    expect(res.status).toBe(200);
    expect(res.body).toEqual({
      totalCalories: 0,
      totalProteinGrams: 0,
      totalFatGrams: 0,
      totalCarbsGrams: 0,
    });
  });

  it("sums calories and macros across all entries for the date", async () => {
    const { app } = buildTestApp();
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-02-01T10:00:00.000Z" });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-02-01T14:00:00.000Z" });

    const res = await request(app).get("/api/summary/daily").query({ date: "2026-02-01" });
    expect(res.body.totalCalories).toBe(496);
  });

  it("reflects an edit made after the meal was saved", async () => {
    const { app } = buildTestApp();
    const created = await request(app)
      .post("/api/meals")
      .send({ items: [sampleItem], occurredAt: "2026-02-02T08:00:00.000Z" });

    await request(app)
      .patch(`/api/meals/${created.body.id}`)
      .send({ items: [{ ...sampleItem, calories: 500 }] });

    const res = await request(app).get("/api/summary/daily").query({ date: "2026-02-02" });
    expect(res.body.totalCalories).toBe(500);
  });
});

describe("GET /api/summary/weekly", () => {
  it("returns zeroed totals and averages for a week with no saved meals", async () => {
    const { app } = buildTestApp();
    const res = await request(app).get("/api/summary/weekly").query({ startDate: "2026-03-01" });
    expect(res.body).toEqual({
      total: { totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
      averagePerDay: { totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
    });
  });

  it("sums entries within the week and averages across all 7 days", async () => {
    const { app } = buildTestApp();
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-03-01T08:00:00.000Z" });
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-03-03T08:00:00.000Z" });
    // Outside the week window - must not be counted.
    await request(app).post("/api/meals").send({ items: [sampleItem], occurredAt: "2026-03-08T08:00:00.000Z" });

    const res = await request(app).get("/api/summary/weekly").query({ startDate: "2026-03-01" });
    expect(res.body.total.totalCalories).toBe(496);
    expect(res.body.averagePerDay.totalCalories).toBeCloseTo(496 / 7);
  });
});
