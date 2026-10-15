import { sqliteTable, integer, real, text, index, uniqueIndex } from "drizzle-orm/sqlite-core";
import { relations } from "drizzle-orm";

export const MEAL_TYPES = ["breakfast", "lunch", "dinner", "snack"] as const;
export type MealType = (typeof MEAL_TYPES)[number];

export const meals = sqliteTable(
  "meals",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    occurredAt: text("occurred_at").notNull(),
    createdAt: text("created_at").notNull(),
    totalCalories: real("total_calories").notNull(),
    totalProteinGrams: real("total_protein_grams").notNull(),
    totalFatGrams: real("total_fat_grams").notNull(),
    totalCarbsGrams: real("total_carbs_grams").notNull(),
    notes: text("notes"),
    mealType: text("meal_type", { enum: MEAL_TYPES }),
    isFavorite: integer("is_favorite", { mode: "boolean" }).notNull().default(false),
    // UUID the client generates per meal, so a retried save returns the stored meal (lower case).
    clientId: text("client_id"),
  },
  (table) => ({ clientIdIdx: uniqueIndex("meals_client_id_idx").on(table.clientId) }),
);

export const mealItems = sqliteTable("meal_items", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  mealId: integer("meal_id")
    .notNull()
    .references(() => meals.id, { onDelete: "cascade" }),
  name: text("name").notNull(),
  estimatedGrams: real("estimated_grams").notNull(),
  confidence: text("confidence", { enum: ["low", "medium", "high"] }).notNull(),
  calories: real("calories").notNull(),
  proteinGrams: real("protein_grams").notNull(),
  fatGrams: real("fat_grams").notNull(),
  carbsGrams: real("carbs_grams").notNull(),
});

/** Single-user app: at most one row, always with id = GOALS_ROW_ID. */
export const GOALS_ROW_ID = 1;

export const goals = sqliteTable("goals", {
  id: integer("id").primaryKey(),
  targetCalories: real("target_calories").notNull(),
  targetProteinGrams: real("target_protein_grams").notNull(),
  targetFatGrams: real("target_fat_grams").notNull(),
  targetCarbsGrams: real("target_carbs_grams").notNull(),
  // Added after the nutrition targets; SQL defaults keep a goal saved before them valid.
  targetWaterMl: integer("target_water_ml").notNull().default(2000),
  waterGlassMl: integer("water_glass_ml").notNull().default(250),
  updatedAt: text("updated_at").notNull(),
});

/** One weighing. `day` is the user's local calendar day (YYYY-MM-DD); several per day are allowed. */
export const weightLogs = sqliteTable(
  "weight_logs",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    weightKg: real("weight_kg").notNull(),
    day: text("day").notNull(),
    createdAt: text("created_at").notNull(),
  },
  (table) => ({ dayIdx: index("weight_logs_day_idx").on(table.day) }),
);

/** One water entry. It belongs to the local calendar day of `loggedAt` (UTC ISO), like meals. */
export const waterLogs = sqliteTable(
  "water_logs",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    amountMl: integer("amount_ml").notNull(),
    loggedAt: text("logged_at").notNull(),
    // Like meals' clientId: a retried log returns the stored entry.
    clientId: text("client_id"),
  },
  (table) => ({
    loggedAtIdx: index("water_logs_logged_at_idx").on(table.loggedAt),
    clientIdIdx: uniqueIndex("water_logs_client_id_idx").on(table.clientId),
  }),
);

export const mealsRelations = relations(meals, ({ many }) => ({
  items: many(mealItems),
}));

export const mealItemsRelations = relations(mealItems, ({ one }) => ({
  meal: one(meals, {
    fields: [mealItems.mealId],
    references: [meals.id],
  }),
}));

export type MealRow = typeof meals.$inferSelect;
export type NewMealRow = typeof meals.$inferInsert;
export type MealItemRow = typeof mealItems.$inferSelect;
export type NewMealItemRow = typeof mealItems.$inferInsert;
export type GoalsRow = typeof goals.$inferSelect;
export type WeightLogRow = typeof weightLogs.$inferSelect;
export type WaterLogRow = typeof waterLogs.$inferSelect;
