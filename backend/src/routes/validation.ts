import type { ZodError } from "zod";
import { ValidationError } from "../errors.js";

/**
 * One readable line per problem ("items.0.calories: Invalid input: expected number"), since the app
 * shows this text to the user as-is. Root-level issues (e.g. a body that isn't an object) have an
 * empty path and are labelled "body".
 */
export function validationErrorFrom(error: ZodError): ValidationError {
  return new ValidationError(
    error.issues.map((issue) => `${issue.path.join(".") || "body"}: ${issue.message}`).join("; "),
  );
}
