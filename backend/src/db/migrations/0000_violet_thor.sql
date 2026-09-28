CREATE TABLE `meal_items` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`meal_id` integer NOT NULL,
	`name` text NOT NULL,
	`estimated_grams` real NOT NULL,
	`confidence` text NOT NULL,
	`calories` real NOT NULL,
	`protein_grams` real NOT NULL,
	`fat_grams` real NOT NULL,
	`carbs_grams` real NOT NULL,
	FOREIGN KEY (`meal_id`) REFERENCES `meals`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `meals` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`occurred_at` text NOT NULL,
	`created_at` text NOT NULL,
	`total_calories` real NOT NULL,
	`total_protein_grams` real NOT NULL,
	`total_fat_grams` real NOT NULL,
	`total_carbs_grams` real NOT NULL,
	`notes` text
);
