CREATE TABLE `water_logs` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`amount_ml` integer NOT NULL,
	`logged_at` text NOT NULL
);
--> statement-breakpoint
CREATE INDEX `water_logs_logged_at_idx` ON `water_logs` (`logged_at`);--> statement-breakpoint
ALTER TABLE `goals` ADD `target_water_ml` integer DEFAULT 2000 NOT NULL;--> statement-breakpoint
ALTER TABLE `goals` ADD `water_glass_ml` integer DEFAULT 250 NOT NULL;