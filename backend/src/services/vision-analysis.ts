import type Anthropic from "@anthropic-ai/sdk";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";
import { AnalysisError } from "../errors.js";
import { logger } from "../logger.js";
import { MealAnalysisSchema, type MealAnalysis } from "./meal-analysis-schema.js";
import type { PreprocessedImage } from "./image-preprocessing.js";

const ANALYSIS_PROMPT = `You are a nutrition analysis assistant. Look at this photo of a meal and identify \
each distinct food item visible. For each item, estimate its quantity in grams and its calories, \
protein (g), fat (g), and carbohydrates (g). If a quantity is hard to judge from the photo, still \
provide your best estimate and set its confidence to "low" rather than omitting the item. If no food \
is visible in the photo, return an empty items list with all totals at zero.`;

/** The languages the analysis texts (item names, notes) can be written in. */
export type AnalysisLanguage = "es" | "en";

/**
 * The requested analysis language, or English for anything missing or unknown - an older client
 * (or curl) without the field must keep working, so this never rejects.
 */
export function parseAnalysisLanguage(value: unknown): AnalysisLanguage {
  return typeof value === "string" && value.trim().toLowerCase() === "es" ? "es" : "en";
}

const LANGUAGE_INSTRUCTIONS: Record<AnalysisLanguage, string> = {
  es: "Write each item's name and the notes in Spanish (es), using common food names a person in a Spanish-speaking country would recognize.",
  en: "Write each item's name and the notes in English.",
};

/** The full instruction sent with the photo. Numbers and enum values stay language-neutral. */
export function analysisPrompt(language: AnalysisLanguage): string {
  return `${ANALYSIS_PROMPT} ${LANGUAGE_INSTRUCTIONS[language]}`;
}

export interface AnalyzeMealPhotoResult {
  analysis: MealAnalysis;
  usage: {
    inputTokens: number;
    outputTokens: number;
  };
}

export async function analyzeMealPhoto(
  client: Anthropic,
  image: PreprocessedImage,
  model: string,
  language: AnalysisLanguage = "en",
): Promise<AnalyzeMealPhotoResult> {
  let response;
  try {
    response = await client.messages.parse({
      model,
      max_tokens: 4096,
      messages: [
        {
          role: "user",
          content: [
            {
              type: "image",
              source: {
                type: "base64",
                media_type: image.mediaType,
                data: image.buffer.toString("base64"),
              },
            },
            { type: "text", text: analysisPrompt(language) },
          ],
        },
      ],
      output_config: { format: zodOutputFormat(MealAnalysisSchema) },
    });
  } catch (error) {
    throw new AnalysisError("Vision analysis request failed", error);
  }

  logger.info("meal_photo_analyzed", {
    model,
    inputTokens: response.usage.input_tokens,
    outputTokens: response.usage.output_tokens,
  });

  if (!response.parsed_output) {
    throw new AnalysisError("Vision analysis response did not match the expected schema");
  }

  return {
    analysis: response.parsed_output,
    usage: {
      inputTokens: response.usage.input_tokens,
      outputTokens: response.usage.output_tokens,
    },
  };
}
