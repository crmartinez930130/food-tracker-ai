import { config } from "../config.js";
import { openDatabase } from "./client.js";

openDatabase(config.dbPath);
console.log(`Migrations applied to ${config.dbPath}`);
