import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import request from "supertest";
import { buildTestApp } from "../helpers/test-app.js";
import { BACKEND_VERSION } from "../../src/version.js";

/** The project version from the repository's version.properties (`version=X.Y.Z`). */
function projectVersion(): string {
  const text = readFileSync(new URL("../../../version.properties", import.meta.url), "utf8");
  const match = /^\s*version\s*=\s*(\S+)\s*$/m.exec(text);
  if (!match) throw new Error("version.properties has no `version=` line");
  return match[1];
}

describe("backend version", () => {
  it("equals the project version in version.properties", () => {
    const expected = projectVersion();
    expect(
      BACKEND_VERSION,
      `backend/package.json has version ${BACKEND_VERSION} but version.properties has ${expected}; update package.json (and package-lock.json)`,
    ).toBe(expected);
  });

  it("is a MAJOR.MINOR.PATCH version", () => {
    expect(BACKEND_VERSION).toMatch(/^\d+\.\d+\.\d+$/);
  });
});

describe("GET /api/version", () => {
  it("returns the backend version with the access token", async () => {
    const { app } = buildTestApp();

    const res = await request(app).get("/api/version");

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ version: BACKEND_VERSION });
  });

  it("is rejected without the access token, without revealing the version", async () => {
    const { app } = buildTestApp(undefined, { authorize: false });

    const res = await request(app).get("/api/version");

    expect(res.status).toBe(401);
    expect(JSON.stringify(res.body)).not.toContain(BACKEND_VERSION);
  });
});
