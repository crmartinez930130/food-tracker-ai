import { readFileSync } from "node:fs";

/**
 * The backend's version, from its package.json (one level above both src/ and dist/). It must equal
 * the project version in the repository's version.properties; a test checks it, because the laptop
 * deploy copies only backend/ and can't read that file.
 */
export const BACKEND_VERSION: string = (
  JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8")) as { version: string }
).version;
