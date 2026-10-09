CREATE TABLE `goals` (
	`id` integer PRIMARY KEY NOT NULL,
	`target_calories` real NOT NULL,
	`target_protein_grams` real NOT NULL,
	`target_fat_grams` real NOT NULL,
	`target_carbs_grams` real NOT NULL,
	`updated_at` text NOT NULL
);
