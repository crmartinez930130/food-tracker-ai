import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";

const goal = { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60, targetCarbsGrams: 170 };
// What a goal saved without water settings carries.
const defaultWater = { targetWaterMl: 2000, waterGlassMl: 250 };

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
      targetWaterMl: 2000,
      waterGlassMl: 250,
      isDefault: true,
    });
  });
});

describe("PUT /api/goals", () => {
  it("saves the goal and returns it", async () => {
    const { app } = buildTestApp();

    const res = await request(app).put("/api/goals").send(goal);

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ...goal, ...defaultWater, isDefault: false });
  });

  it("is reflected by a subsequent GET", async () => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);

    const res = await request(app).get("/api/goals");

    expect(res.body).toEqual({ ...goal, ...defaultWater, isDefault: false });
  });

  it("replaces a previously saved goal", async () => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);
    const replacement = { targetCalories: 2500, targetProteinGrams: 180, targetFatGrams: 80, targetCarbsGrams: 270 };

    await request(app).put("/api/goals").send(replacement);
    const res = await request(app).get("/api/goals");

    expect(res.body).toEqual({ ...replacement, ...defaultWater, isDefault: false });
  });

  it.each([
    ["a missing target", { targetCalories: 1800, targetProteinGrams: 140, targetFatGrams: 60 }],
    ["zero calories", { ...goal, targetCalories: 0 }],
    ["a negative macro", { ...goal, targetProteinGrams: -5 }],
    ["a non-numeric target", { ...goal, targetFatGrams: "lots" }],
    ["a zero water target", { ...goal, targetWaterMl: 0 }],
    ["a water target over 20000 ml", { ...goal, targetWaterMl: 20001 }],
    ["a fractional water target", { ...goal, targetWaterMl: 1500.5 }],
    ["a zero glass size", { ...goal, waterGlassMl: 0 }],
    ["a glass size over 5000 ml", { ...goal, waterGlassMl: 5001 }],
    ["a non-numeric glass size", { ...goal, waterGlassMl: "big" }],
  ])("rejects %s with 400 and leaves the stored goal unchanged", async (_label, body) => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send(goal);

    const res = await request(app).put("/api/goals").send(body);
    const after = await request(app).get("/api/goals");

    expect(res.status).toBe(400);
    expect(res.body.error).toEqual(expect.any(String));
    expect(after.body).toEqual({ ...goal, ...defaultWater, isDefault: false });
  });

  it("returns a readable error naming the offending field", async () => {
    const { app } = buildTestApp();

    const res = await request(app).put("/api/goals").send({ ...goal, targetCalories: 0 });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/^targetCalories: /);
    expect(res.body.error).not.toContain("{");
  });

  it("saves the water target and glass size", async () => {
    const { app } = buildTestApp();

    const res = await request(app).put("/api/goals").send({ ...goal, targetWaterMl: 2500, waterGlassMl: 330 });
    const after = await request(app).get("/api/goals");

    expect(res.status).toBe(200);
    expect(after.body).toEqual({ ...goal, targetWaterMl: 2500, waterGlassMl: 330, isDefault: false });
  });

  it("keeps the stored water settings when a save sends only the nutrition targets", async () => {
    const { app } = buildTestApp();
    await request(app).put("/api/goals").send({ ...goal, targetWaterMl: 2500, waterGlassMl: 330 });

    await request(app).put("/api/goals").send({ ...goal, targetCalories: 2100 });
    const after = await request(app).get("/api/goals");

    expect(after.body).toEqual({ ...goal, targetCalories: 2100, targetWaterMl: 2500, waterGlassMl: 330, isDefault: false });
  });

  it("accepts the water bounds", async () => {
    const { app } = buildTestApp();

    expect((await request(app).put("/api/goals").send({ ...goal, targetWaterMl: 1, waterGlassMl: 1 })).status).toBe(200);
    expect((await request(app).put("/api/goals").send({ ...goal, targetWaterMl: 20000, waterGlassMl: 5000 })).status).toBe(200);
  });
});
