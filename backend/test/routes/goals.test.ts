import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";

const goal = { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60, targetCarbsGrams: 170 };

describe("GET /api/goals", () => {
  it("returns the defaults flagged as default when no goal was saved", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/goals");

    expect(res.status).toBe(200);
    expect(res.body).toEqual({
      targetCalories: 2000,
      targetProteinGrams: 125,
      targetFatGrams: 67,
      targetCarbsGrams: 225,
      isDefault: true,
    });
  });
});

describe("PUT /api/goals", () => {
  it("saves the goal and returns it", async () => {
    const { app } = buildTestApp();

    const res = await request(app).put("/api/goals").send(goal);

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ...goal, isDefault: false });
  });

  it("is reflected by a subsequent GET", async () => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);

    const res = await request(app).get("/api/goals");

    expect(res.body).toEqual({ ...goal, isDefault: false });
  });

  it("replaces a previously saved goal", async () => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);
    const replacement = { targetCalories: 2500, targetProteinGrams: 180, targetFatGrams: 80, targetCarbsGrams: 270 };

    await request(app).put("/api/goals").send(replacement);
    const res = await request(app).get("/api/goals");

    expect(res.body).toEqual({ ...replacement, isDefault: false });
  });

  it.each([
    ["a missing target", { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60 }],
    ["zero calories", { ...goal, targetCalories: 0 }],
    ["a negative macro", { ...goal, targetProteinGrams: -5 }],
    ["a non-numeric target", { ...goal, targetFatGrams: "lots" }],
  ])("rejects %s with 400 and leaves the stored goal unchanged", async (_label, body) => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);

    const res = await request(app).put("/api/goals").send(body);
    const after = await request(app).get("/api/goals");

    expect(res.status).toBe(400);
    expect(res.body.error).toEqual(expect.any(String));
    expect(after.body).toEqual({ ...goal, isDefault: false });
  });

  it("returns a readable error naming the offending field", async () => {
    const { app } = buildTestApp();

    const res = await request(app).put("/api/goals").send({ ...goal, targetCalories: 0 });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^targetCalories: /);
    expect(res.body.error).not.toContain("{");
  });
});
