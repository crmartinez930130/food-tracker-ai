import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";
import { localToday } from "../../src/routes/dates.js";

describe("POST /api/weight", () => {
  it("logs a weight for today when no day is given", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/weight").send({ weightKg: 72.4 });

    expect(res.status).toBe(201);
    expect(res.body).toMatchObject({ weightKg: 72.4, day: localToday() });
    expect(res.body.id).toBeTypeOf("number");
  });

  it("logs a weight for an earlier day", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/weight").send({ weightKg: 71.8, day: "2026-01-15" });

    expect(res.status).toBe(201);
    expect(res.body.day).toBe("2026-01-15");
  });

  it.each([
    ["zero weight", { weightKg: 0 }],
    ["a negative weight", { weightKg: -3 }],
    ["more than 500 kg", { weightKg: 600 }],
    ["a non-numeric weight", { weightKg: "72" }],
    ["a missing weight", {}],
    ["a malformed day", { weightKg: 72, day: "2026-1-5" }],
    ["an impossible day", { weightKg: 72, day: "2026-02-30" }],
    ["a future day", { weightKg: 72, day: "2999-01-01" }],
  ])("rejects %s with 400 and stores nothing", async (_label, body) => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/weight").send(body);
    const stored = await request(app).get("/api/weight").query({ startDate: "2026-01-01", endDate: localToday() });

    expect(res.status).toBe(400);
    expect(res.body.error).toEqual(expect.any(String));
    expect(res.body.error).not.toContain("{");
    expect(stored.body).toEqual([]);
  });
});

describe("GET /api/weight", () => {
  it("returns only the entries in the inclusive range, oldest first", async () => {
    const { app } = buildTestApp();
    for (const [weightKg, day] of [
      [73, "2026-03-03"],
      [72, "2026-03-01"],
      [71, "2026-02-27"],
      [74, "2026-03-05"],
    ] as const) {
      await request(app).post("/api/weight").send({ weightKg, day });
    }

    const res = await request(app).get("/api/weight").query({ startDate: "2026-03-01", endDate: "2026-03-03" });

    expect(res.status).toBe(200);
    expect(res.body.map((e: { day: string; weightKg: number }) => [e.day, e.weightKg])).toEqual([
      ["2026-03-01", 72],
      ["2026-03-03", 73],
    ]);
  });

  it("returns an empty list for a range without entries", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/weight").query({ startDate: "2026-03-01", endDate: "2026-03-31" });

    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it.each([
    ["a missing parameter", { startDate: "2026-03-01" }],
    ["an end before the start", { startDate: "2026-03-05", endDate: "2026-03-01" }],
    ["a 367-day range", { startDate: "2026-01-01", endDate: "2027-01-02" }],
  ])("rejects %s with 400", async (_label, query) => {
    const { app } = buildTestApp();

    expect((await request(app).get("/api/weight").query(query)).status).toBe(400);
  });
});

describe("DELETE /api/weight/:id", () => {
  it("deletes an entry so it no longer appears", async () => {
    const { app } = buildTestApp();
    const created = await request(app).post("/api/weight").send({ weightKg: 72, day: "2026-03-01" });

    const res = await request(app).delete(`/api/weight/${created.body.id}`);
    const after = await request(app).get("/api/weight").query({ startDate: "2026-03-01", endDate: "2026-03-01" });

    expect(res.status).toBe(204);
    expect(after.body).toEqual([]);
  });

  it("returns 404 for an id that doesn't exist", async () => {
    const { app } = buildTestApp();

    expect((await request(app).delete("/api/weight/999")).status).toBe(404);
  });
});
