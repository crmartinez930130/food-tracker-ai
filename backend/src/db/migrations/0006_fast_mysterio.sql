ALTER TABLE `meals` ADD `client_id` text;--> statement-breakpoint
CREATE UNIQUE INDEX `meals_client_id_idx` ON `meals` (`client_id`);--> statement-breakpoint
ALTER TABLE `water_logs` ADD `client_id` text;--> statement-breakpoint
CREATE UNIQUE INDEX `water_logs_client_id_idx` ON `water_logs` (`client_id`);