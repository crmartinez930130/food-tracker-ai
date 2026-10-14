import { describe, expect, it, vi } from "vitest";
import request from "supertest";
import { TEST_API_TOKEN, buildTestApp } from "../helpers/test-app.js";
import { createRateLimiter } from "../../src/http/rate-limit.js";

const ROUTES: Array<[string, string]> = [
  ["get", "/api/goals"],
  ["get", "/api/meals?date=2026-02-01"],
  ["get", "/api/meals/export?startDate=2026-02-01&endDate=2026-02-02"],
  ["get", "/api/summary/range?startDate=2026-02-01&endDate=2026-02-02"],
  ["get", "/api/water?date=2026-02-01"],
  ["get", "/api/weight?startDate=2026-02-01&endDate=2026-02-02"],
  ["post", "/api/meals"],
  ["post", "/api/meals/analyze"],
  ["delete", "/api/meals/1"],
  ["put", "/api/goals"],
];

describe("access token", () => {
  it("rejects every API route without a token with 401", async () => {
    const parse = vi.fn();
    const { app } = buildTestApp(parse, { authorize: false });

    for (const [method, path] of ROUTES) {
      const res = await (request(app) as unknown as Record<string, (p: string) => request.Test>)[method](path);
      expect(res.status, `${method} ${path}`).toBe(401);
      expect(res.body).toEqual({ error: "Missing or invalid access token" });
      expect(res.headers["www-authenticate"]).toBe("Bearer");
    }
    expect(parse).not.toHaveBeenCalled();
  });

  it("rejects a wrong token or scheme and changes nothing", async () => {
    const { app } = buildTestApp(undefined, { authorize: false });

    const wrong = await request(app).post("/api/water").set("Authorization", "Bearer not-the-token-123456").send({ amountMl: 250 });
    const basic = await request(app).get("/api/goals").set("Authorization", `Basic ${TEST_API_TOKEN}`);

    expect(wrong.status).toBe(401);
    expect(basic.status).toBe(401);
    const ok = await request(app).get("/api/water?date=2026-02-01").set("Authorization", `Bearer ${TEST_API_TOKEN}`);
    expect(ok.status).toBe(200);
    // The rejected POST stored nothing.
    expect(ok.body.totalMl).toBe(0);
  });

  it("serves requests with the right token and hides X-Powered-By", async () => {
    const { app } = buildTestApp(undefined, { authorize: false });

    const res = await request(app).get("/api/goals").set("Authorization", `Bearer ${TEST_API_TOKEN}`);

    expect(res.status).toBe(200);
    expect(res.headers["x-powered-by"]).toBeUndefined();
  });

  it("rejects a cross-site form post to the analysis (no token can be attached)", async () => {
    const parse = vi.fn();
    const { app } = buildTestApp(parse, { authorize: false });

    const res = await request(app).post("/api/meals/analyze").field("description", "una manzana");

    expect(res.status).toBe(401);
    expect(parse).not.toHaveBeenCalled();
  });
});

describe("analysis rate limit", () => {
  it("answers 429 with Retry-After past the limit, without calling the analysis", async () => {
    const parse = vi.fn(async () => ({
      parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
      usage: { input_tokens: 1, output_tokens: 1 },
    }));
    const { app } = buildTestApp(parse, { analyzeLimiter: createRateLimiter({ max: 1, windowMs: 60_000 }) });

    const first = await request(app).post("/api/meals/analyze").field("description", "una manzana");
    const second = await request(app).post("/api/meals/analyze").field("description", "una pera");

    expect(first.status).toBe(200);
    expect(second.status).toBe(429);
    expect(Number(second.headers["retry-after"])).toBeGreaterThan(0);
    expect(second.body.error).toMatch(/Too many analyses/);
    expect(parse).toHaveBeenCalledTimes(1);
  });

  it("doesn't limit other routes", async () => {
    const { app } = buildTestApp(undefined, { analyzeLimiter: createRateLimiter({ max: 0, windowMs: 60_000 }) });

    expect((await request(app).get("/api/goals")).status).toBe(200);
  });
});
