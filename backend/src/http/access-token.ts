import { createHash, timingSafeEqual } from "node:crypto";
import type { NextFunction, Request, Response } from "express";

/** Shortest accepted token: long enough that guessing it over the network is hopeless. */
export const MIN_TOKEN_LENGTH = 16;

/** The placeholder in .env.example: public, so it must never work as a real token. */
export const EXAMPLE_TOKEN = "change-me-to-a-random-value";

const HOW_TO = "Set API_TOKEN in backend/.env to a random value, e.g. the output of `openssl rand -hex 24`.";

/** The configured access token, or an error explaining how to set one. */
export function parseApiToken(value: string | undefined): string {
  const token = value?.trim() ?? "";
  if (!token) throw new Error(`Missing required environment variable: API_TOKEN. ${HOW_TO}`);
  if (token === EXAMPLE_TOKEN) throw new Error(`API_TOKEN is still the example value. ${HOW_TO}`);
  if (token.length < MIN_TOKEN_LENGTH) {
    throw new Error(`API_TOKEN must be at least ${MIN_TOKEN_LENGTH} characters. ${HOW_TO}`);
  }
  return token;
}

const digest = (value: string) => createHash("sha256").update(value, "utf8").digest();

/**
 * True when [header] is `Bearer <expected>`. Both sides are hashed first, so the comparison is
 * over equal-length buffers and its timing reveals neither the token's length nor its content.
 */
export function hasValidToken(header: string | undefined, expected: string): boolean {
  const match = /^Bearer\s+(.+)$/i.exec(header ?? "");
  const given = match?.[1]?.trim() ?? "";
  return timingSafeEqual(digest(given), digest(expected)) && given.length > 0;
}

/** Rejects any request without the owner's token, before it reaches a router. */
export function requireAccessToken(expected: string) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (hasValidToken(req.headers.authorization, expected)) {
      next();
      return;
    }
    res.setHeader("WWW-Authenticate", "Bearer");
    res.status(401).json({ error: "Missing or invalid access token" });
  };
}
