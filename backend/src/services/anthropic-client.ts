import Anthropic from "@anthropic-ai/sdk";
import { config } from "../config.js";

export function createAnthropicClient(): Anthropic {
  return new Anthropic({ apiKey: config.anthropicApiKey });
}
