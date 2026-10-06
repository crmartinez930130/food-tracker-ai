import { sqliteTable, integer, real, text } from "drizzle-orm/sqlite-core";
import { relations } from "drizzle-orm";

export const MEAL_TYPES = ["breakfast", "lunch", "dinner", "snack"] as const;
export type MealType = (typeof MEAL_TYPES)[number];

export const meals = sqliteTable("meals", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  occurredAt: text("occurred_at").notNull(),
  createdAt: text("created_at").notNull(),
  totalCalories: real("total_calories").notNull(),
  totalProteinGrams: real("total_protein_grams").notNull(),
  totalFatGrams: real("total_fat_grams").notNull(),
  totalCarbsGrams: real("total_carbs_grams").notNull(),
  notes: text("notes"),
  mealType: text("meal_type", { enum: MEAL_TYPES }),
});

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
