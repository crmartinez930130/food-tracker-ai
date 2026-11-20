import { McpServer, type CallToolResult } from "@modelcontextprotocol/server";
import * as z from "zod/v4";
import type { AppDatabase } from "../db/client.js";
import { MEAL_TYPES } from "../db/schema.js";
import { getDailyGoal } from "../db/goals-repository.js";
import { firstMealDate, getMealById, listFavoriteMeals, listMealsBetween } from "../db/meal-repository.js";
import { listWeightLogs } from "../db/weight-repository.js";
import { waterForDay, waterTotalsBetween } from "../db/water-repository.js";
import { addDaysToDateString, rangeSummary } from "../services/summary.js";
import { checkRangeLength, isCalendarDate, localToday, MAX_RANGE_DAYS } from "../routes/dates.js";
import { NotFoundError, ValidationError } from "../errors.js";
import { logger } from "../logger.js";
import { BACKEND_VERSION } from "../version.js";

/** Longest range for the tools that return every meal (with its items) or every water entry. */
export const MAX_DETAIL_RANGE_DAYS = 31;

/** Every tool only reads, so calling it again with the same arguments gives the same answer. */
const READ_ONLY = { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: false } as const;

const date = (what: string) => z.string().describe(`${what}, as YYYY-MM-DD (the owner's local calendar day)`);
const rangeInput = z.object({ startDate: date("First day, included"), endDate: date("Last day, included") });

function requireDate(value: string, name: string): string {
  if (!isCalendarDate(value)) {
    throw new ValidationError(`'${name}' must be a real date in YYYY-MM-DD format`);
  }
  return value;
}

function requireRange(args: { startDate: string; endDate: string }, maxDays: number) {
  const startDate = requireDate(args.startDate, "startDate");
  const endDate = requireDate(args.endDate, "endDate");
  checkRangeLength(startDate, endDate, maxDays);
  return { startDate, endDate };
}

function errorResult(message: string): CallToolResult {
  return { isError: true, content: [{ type: "text", text: message }] };
}

/**
 * Runs a tool body and turns its value into a JSON result (structured content plus the same JSON
 * as text, for clients that only read text). Bad arguments and missing data become tool errors
 * with their message; anything else is logged and reported without internal details.
 */
function run(tool: string, body: () => Record<string, unknown>): CallToolResult {
  try {
    const data = body();
    return { content: [{ type: "text", text: JSON.stringify(data) }], structuredContent: data };
  } catch (err) {
    if (err instanceof ValidationError || err instanceof NotFoundError) return errorResult(err.message);
    logger.error("mcp_tool_error", { tool, message: err instanceof Error ? err.message : String(err) });
    return errorResult("Internal error while reading the data");
  }
}

/** The read-only MCP server over [db]: one per request, since the endpoint is stateless. */
export function createMcpServer(db: AppDatabase): McpServer {
  const server = new McpServer(
    { name: "food-tracker", version: BACKEND_VERSION },
    {
      instructions:
        "Read-only access to one person's food tracker: meals with their items and macros, daily " +
        "nutrition and water goals, weight and water logs. Dates are the owner's local calendar days " +
        "(YYYY-MM-DD); energy in kcal, macros in grams, water in ml, weight in kg. Call get_data_range " +
        "first to learn which days have data.",
    },
  );

  server.registerTool(
    "get_goals",
    {
      title: "Daily goals",
      description:
        "The daily targets: calories (kcal), protein, fat and carbs (g), water (ml) and the size of one glass of water (ml). `isDefault` is true when the owner never set goals and these are the built-in defaults.",
      annotations: READ_ONLY,
    },
    () => run("get_goals", () => ({ ...getDailyGoal(db) })),
  );

  server.registerTool(
    "get_data_range",
    {
      title: "Available data range",
      description:
        "The local day of the earliest logged meal (null when there are none) and today's date on the server, so you know which days can be queried.",
      annotations: READ_ONLY,
    },
    () => run("get_data_range", () => ({ firstMealDate: firstMealDate(db), today: localToday() })),
  );

  server.registerTool(
    "get_daily_summary",
    {
      title: "Daily summary",
      description:
        "One day's totals: calories, protein, fat and carbs from all its meals, the meal count and the water drunk (ml), together with the daily goals.",
      inputSchema: z.object({ date: date("The day") }),
      annotations: READ_ONLY,
    },
    (args) =>
      run("get_daily_summary", () => {
        const day = requireDate(args.date, "date");
        const [totals] = rangeSummary(db, day, day);
        return { ...totals, goals: getDailyGoal(db) };
      }),
  );

  server.registerTool(
    "get_range_summary",
    {
      title: "Summary per day",
      description: `Totals per day from startDate to endDate (both included, at most ${MAX_RANGE_DAYS} days): calories, protein, fat, carbs, meal count and water (ml). Days without data are included with zeros.`,
      inputSchema: rangeInput,
      annotations: READ_ONLY,
    },
    (args) =>
      run("get_range_summary", () => {
        const { startDate, endDate } = requireRange(args, MAX_RANGE_DAYS);
        return { days: rangeSummary(db, startDate, endDate) };
      }),
  );

  server.registerTool(
    "list_meals",
    {
      title: "Meals",
      description: `The meals logged from startDate to endDate (both included, at most ${MAX_DETAIL_RANGE_DAYS} days), oldest first, each with its type, notes, favorite flag, totals and items (name, estimated grams, confidence, calories and macros). Optionally only one meal type. For longer periods use get_range_summary.`,
      inputSchema: rangeInput.extend({
        mealType: z.enum(MEAL_TYPES).optional().describe("Only meals of this type"),
      }),
      annotations: READ_ONLY,
    },
    (args) =>
      run("list_meals", () => {
        const { startDate, endDate } = requireRange(args, MAX_DETAIL_RANGE_DAYS);
        const meals = listMealsBetween(db, startDate, addDaysToDateString(endDate, 1))
          .filter((meal) => !args.mealType || meal.mealType === args.mealType)
          .sort((a, b) => a.occurredAt.localeCompare(b.occurredAt) || a.id - b.id);
        return { meals };
      }),
  );

  server.registerTool(
    "get_meal",
    {
      title: "One meal",
      description: "One meal by its id, with its items, totals, type, notes and favorite flag.",
      inputSchema: z.object({ id: z.number().int().positive().describe("The meal's id") }),
      annotations: READ_ONLY,
    },
    (args) =>
      run("get_meal", () => {
        const meal = getMealById(db, args.id);
        if (!meal) throw new NotFoundError(`Meal ${args.id} not found`);
        return { ...meal };
      }),
  );

  server.registerTool(
    "list_favorite_meals",
    {
      title: "Favorite meals",
      description: "The meals the owner marked as favorite, newest first, with their items and totals.",
      annotations: READ_ONLY,
    },
    () => run("list_favorite_meals", () => ({ meals: listFavoriteMeals(db) })),
  );

  server.registerTool(
    "list_weight_logs",
    {
      title: "Weight logs",
      description: `The weighings from startDate to endDate (both included, at most ${MAX_RANGE_DAYS} days), oldest first: day, weight in kg and when it was logged. A day may have several.`,
      inputSchema: rangeInput,
      annotations: READ_ONLY,
    },
    (args) =>
      run("list_weight_logs", () => {
        const { startDate, endDate } = requireRange(args, MAX_RANGE_DAYS);
        return { logs: listWeightLogs(db, startDate, endDate) };
      }),
  );

  server.registerTool(
    "list_water_logs",
    {
      title: "Water logs",
      description: `The water logged from startDate to endDate (both included, at most ${MAX_DETAIL_RANGE_DAYS} days): for each day with entries, its total (ml) and its entries (amount and time), oldest first.`,
      inputSchema: rangeInput,
      annotations: READ_ONLY,
    },
    (args) =>
      run("list_water_logs", () => {
        const { startDate, endDate } = requireRange(args, MAX_DETAIL_RANGE_DAYS);
        const days = waterTotalsBetween(db, startDate, addDaysToDateString(endDate, 1)).map((day) => {
          const { totalMl, entries } = waterForDay(db, day.date);
          return { date: day.date, totalMl, entries: entries.map(({ clientId: _clientId, ...entry }) => entry) };
        });
        return { days };
      }),
  );

  return server;
}
