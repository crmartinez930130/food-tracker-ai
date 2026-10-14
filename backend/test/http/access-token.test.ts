import { describe, expect, it } from "vitest";
import { hasValidToken, parseApiToken } from "../../src/http/access-token.js";

describe("parseApiToken", () => {
  it("rejects a missing or blank token with setup instructions", () => {
    expect(() => parseApiToken(undefined)).toThrow(/API_TOKEN.*openssl rand -hex 24/);
    expect(() => parseApiToken("   ")).toThrow(/Missing required environment variable: API_TOKEN/);
  });

  it("rejects a token shorter than 16 characters", () => {
    expect(() => parseApiToken("short-token-15ch")).not.toThrow();
    expect(() => parseApiToken("short-token-15c")).toThrow(/at least 16 characters/);
  });

  it("rejects the example value from .env.example", () => {
    expect(() => parseApiToken("change-me-to-a-random-value")).toThrow(/still the example value/);
  });

  it("accepts and trims a long enough token", () => {
    expect(parseApiToken("  0123456789abcdef0123  ")).toBe("0123456789abcdef0123");
  });
});

describe("hasValidToken", () => {
  const token = "0123456789abcdef0123";

  it("accepts the Bearer token, with any case of the scheme", () => {
    expect(hasValidToken(`Bearer ${token}`, token)).toBe(true);
    expect(hasValidToken(`bearer ${token}`, token)).toBe(true);
  });

  it("rejects a missing header, another scheme, an empty or a different token", () => {
    expect(hasValidToken(undefined, token)).toBe(false);
    expect(hasValidToken(`Basic ${token}`, token)).toBe(false);
    expect(hasValidToken("Bearer ", token)).toBe(false);
    expect(hasValidToken("Bearer 0123456789abcdef0124", token)).toBe(false);
    expect(hasValidToken(`Bearer ${token}x`, token)).toBe(false);
  });
});
