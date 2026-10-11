import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";

const localToday = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
};

describe("POST /api/water", () => {
  it("logs an amount now, reflected in today's total", async () => {
    const { app } = buildTestApp();
    const before = Date.now();

    const res = await request(app).post("/api/water").send({ amountMl: 250 });
    const day = await request(app).get("/api/water").query({ date: localToday() });

    expect(res.status).toBe(201);
    expect(res.body).toMatchObject({ id: expect.any(Number), amountMl: 250 });
    expect(Date.parse(res.body.loggedAt)).toBeGreaterThanOrEqual(before - 1000);
    expect(day.body.totalMl).toBe(250);
    expect(day.body.entries).toEqual([res.body]);
  });

  it("logs into a past day when loggedAt is given, not into today", async () => {
    const { app } = buildTestApp();
    const loggedAt = new Date(2026, 2, 5, 12, 0).toISOString();

    const res = await request(app).post("/api/water").send({ amountMl: 330, loggedAt });

    expect(res.status).toBe(201);
    expect(res.body.loggedAt).toBe(loggedAt);
    expect((await request(app).get("/api/water").query({ date: "2026-03-05" })).body.totalMl).toBe(330);
  });

  it("normalizes a loggedAt with an offset to UTC", async () => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/water").send({ amountMl: 250, loggedAt: "2026-03-05T12:00:00-03:00" });

    expect(res.status).toBe(201);
    expect(res.body.loggedAt).toBe("2026-03-05T15:00:00.000Z");
  });

  it("tolerates a little clock skew", async () => {
    const { app } = buildTestApp();
    const loggedAt = new Date(Date.now() + 60 * 1000).toISOString();

    expect((await request(app).post("/api/water").send({ amountMl: 250, loggedAt })).status).toBe(201);
  });

  it.each([
    ["a missing amount", {}],
    ["a zero amount", { amountMl: 0 }],
    ["a negative amount", { amountMl: -250 }],
    ["a fractional amount", { amountMl: 250.5 }],
    ["an amount over 5000 ml", { amountMl: 5001 }],
    ["a non-numeric amount", { amountMl: "a glass" }],
    ["a malformed loggedAt", { amountMl: 250, loggedAt: "yesterday" }],
    ["a loggedAt without a time", { amountMl: 250, loggedAt: "2026-03-05" }],
    ["a future loggedAt", { amountMl: 250, loggedAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() }],
  ])("rejects %s with 400 and stores nothing", async (_label, body) => {
    const { app } = buildTestApp();

    const res = await request(app).post("/api/water").send(body);
    const today = await request(app).get("/api/water").query({ date: localToday() });

    expect(res.status).toBe(400);
    expect(res.body.error).toEqual(expect.any(String));
    expect(today.body.totalMl).toBe(0);
  });
});

describe("GET /api/water", () => {
  it("returns 0 and no entries for a day without water", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/water").query({ date: "2026-03-05" });

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ date: "2026-03-05", totalMl: 0, entries: [] });
  });

  it("returns the day's entries oldest first with their total", async () => {
    const { app } = buildTestApp();
    for (const [h, ml] of [[18, 500], [8, 250], [12, 250]]) {
      await request(app).post("/api/water").send({ amountMl: ml, loggedAt: new Date(2026, 2, 5, h).toISOString() });
    }

    const res = await request(app).get("/api/water").query({ date: "2026-03-05" });

    expect(res.body.totalMl).toBe(1000);
    expect(res.body.entries.map((e: { amountMl: number }) => e.amountMl)).toEqual([250, 250, 500]);
  });

  it.each([["a missing date", {}], ["an impossible date", { date: "2026-02-30" }], ["a malformed date", { date: "05/03/2026" }]])(
    "rejects %s with 400",
    async (_label, query) => {
      const { app } = buildTestApp();

      expect((await request(app).get("/api/water").query(query)).status).toBe(400);
    },
  );
});

describe("DELETE /api/water/:id", () => {
  it("removes the entry and the total is recalculated", async () => {
    const { app } = buildTestApp();
    const loggedAt = new Date(2026, 2, 5, 9).toISOString();
    await request(app).post("/api/water").send({ amountMl: 250, loggedAt });
    const mistake = await request(app).post("/api/water").send({ amountMl: 250, loggedAt });

    const res = await request(app).delete(`/api/water/${mistake.body.id}`);
    const day = await request(app).get("/api/water").query({ date: "2026-03-05" });

    expect(res.status).toBe(204);
    expect(day.body.totalMl).toBe(250);
    expect(day.body.entries).toHaveLength(1);
  });

  it("returns 404 for an unknown entry and 400 for an invalid id", async () => {
    const { app } = buildTestApp();

    expect((await request(app).delete("/api/water/999")).status).toBe(404);
    expect((await request(app).delete("/api/water/abc")).status).toBe(400);
    expect((await request(app).delete("/api/water/0")).status).toBe(400);
  });
});
