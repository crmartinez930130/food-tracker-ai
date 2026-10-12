import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";

// A well-formed but impossible date must not be blamed on the format.
describe("impossible dates get the 'real date' message on every route", () => {
  it.each([
    ["/api/summary/daily", { date: "2026-02-30" }, "Query parameter 'date' must be a real date in YYYY-MM-DD format"],
    ["/api/summary/weekly", { startDate: "2026-02-30" }, "Query parameter 'startDate' must be a real date in YYYY-MM-DD format"],
    ["/api/meals", { date: "2026-02-30" }, "Query parameter 'date' must be a real date in YYYY-MM-DD format"],
    ["/api/water", { date: "2026-02-30" }, "Query parameter 'date' must be a real date in YYYY-MM-DD format"],
    [
      "/api/summary/range",
      { startDate: "2026-02-30", endDate: "2026-03-03" },
      "Query parameters 'startDate' and 'endDate' must be real dates in YYYY-MM-DD format",
    ],
    [
      "/api/meals/export",
      { startDate: "2026-02-28", endDate: "2026-02-30" },
      "Query parameters 'startDate' and 'endDate' must be real dates in YYYY-MM-DD format",
    ],
    [
      "/api/weight",
      { startDate: "2026-02-30", endDate: "2026-03-03" },
      "Query parameters 'startDate' and 'endDate' must be real dates in YYYY-MM-DD format",
    ],
  ])("%s", async (path, query, message) => {
    const { app } = buildTestApp();

    const res = await request(app).get(path).query(query);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe(message);
  });
});
