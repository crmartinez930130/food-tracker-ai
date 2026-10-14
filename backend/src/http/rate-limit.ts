import type { NextFunction, Request, Response } from "express";

export type RateLimitDecision = { allowed: true } | { allowed: false; retryAfterSeconds: number };

export interface RateLimiter {
  tryAcquire(): RateLimitDecision;
}

/**
 * A sliding window: at most [max] accepted calls in any [windowMs]. Global, not per client - the
 * backend has one user, and the limit is about cost and runaway clients.
 */
export function createRateLimiter({ max, windowMs, now = Date.now }: { max: number; windowMs: number; now?: () => number }): RateLimiter {
  const accepted: number[] = [];
  return {
    tryAcquire() {
      const current = now();
      while (accepted.length > 0 && accepted[0] <= current - windowMs) accepted.shift();
      if (accepted.length < max) {
        accepted.push(current);
        return { allowed: true };
      }
      const retryAfterMs = accepted[0] + windowMs - current;
      return { allowed: false, retryAfterSeconds: Math.max(1, Math.ceil(retryAfterMs / 1000)) };
    },
  };
}

/** 429 with Retry-After once the limiter says no; mounted before the upload is parsed. */
export function rateLimit(limiter: RateLimiter) {
  return (_req: Request, res: Response, next: NextFunction) => {
    const decision = limiter.tryAcquire();
    if (decision.allowed) {
      next();
      return;
    }
    res.setHeader("Retry-After", String(decision.retryAfterSeconds));
    res.status(429).json({ error: `Too many analyses, try again in ${decision.retryAfterSeconds} s` });
  };
}

/** The analyses-per-minute setting: a positive whole number, 20 when unset. */
export function parseAnalyzeRateLimit(value: string | undefined): number {
  if (value === undefined || value.trim() === "") return 20;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new Error("ANALYZE_RATE_LIMIT_PER_MINUTE must be a positive whole number");
  }
  return parsed;
}
