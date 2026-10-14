import { describe, expect, it } from "vitest";
import { createRateLimiter, parseAnalyzeRateLimit } from "../../src/http/rate-limit.js";

describe("createRateLimiter", () => {
  it("allows up to max calls in a window, then says when to retry", () => {
    let now = 0;
    const limiter = createRateLimiter({ max: 2, windowMs: 60_000, now: () => now });

    expect(limiter.tryAcquire()).toEqual({ allowed: true });
    now = 10_000;
    expect(limiter.tryAcquire()).toEqual({ allowed: true });
    now = 15_000;
    expect(limiter.tryAcquire()).toEqual({ allowed: false, retryAfterSeconds: 45 });
  });

  it("slides: a call leaves the window a full window after it was accepted", () => {
    let now = 0;
    const limiter = createRateLimiter({ max: 1, windowMs: 60_000, now: () => now });

    limiter.tryAcquire();
    now = 59_999;
    expect(limiter.tryAcquire().allowed).toBe(false);
    now = 60_000;
    expect(limiter.tryAcquire()).toEqual({ allowed: true });
  });

  it("rejected calls don't count", () => {
    let now = 0;
    const limiter = createRateLimiter({ max: 1, windowMs: 1_000, now: () => now });

    limiter.tryAcquire();
    now = 500;
    limiter.tryAcquire();
    now = 1_000;
    expect(limiter.tryAcquire()).toEqual({ allowed: true });
  });
});

describe("parseAnalyzeRateLimit", () => {
  it("defaults to 20 and accepts positive whole numbers", () => {
    expect(parseAnalyzeRateLimit(undefined)).toBe(20);
    expect(parseAnalyzeRateLimit("")).toBe(20);
    expect(parseAnalyzeRateLimit("5")).toBe(5);
  });

  it("rejects anything else", () => {
    for (const value of ["0", "-1", "2.5", "abc"]) {
      expect(() => parseAnalyzeRateLimit(value), value).toThrow(/positive whole number/);
    }
  });
});
