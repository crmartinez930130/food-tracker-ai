import { describe, expect, it, vi } from "vitest";
import type Anthropic from "@anthropic-ai/sdk";
import { analyzeMealPhoto } from "../../src/services/vision-analysis.js";
import { AnalysisError } from "../../src/errors.js";
import type { PreprocessedImage } from "../../src/services/image-preprocessing.js";

const sampleImage: PreprocessedImage = {
  buffer: Buffer.from("fake-jpeg-bytes"),
  mediaType: "image/jpeg",
  width: 800,
  height: 600,
};

function fakeClient(parseImpl: (...args: unknown[]) => unknown): Anthropic {
  return { messages: { parse: parseImpl } } as unknown as Anthropic;
}

describe("analyzeMealPhoto", () => {
  it("returns the parsed analysis and usage on success", async () => {
    const parsedAnalysis = {
      items: [
        {
          name: "Grilled chicken breast",
          estimatedGrams: 150,
          confidence: "high" as const,
          calories: 248,
          proteinGrams: 46.5,
          fatGrams: 5.4,
          carbsGrams: 0,
        },
      ],
      totalCalories: 248,
      totalProteinGrams: 46.5,
      totalFatGrams: 5.4,
      totalCarbsGrams: 0,
    };

    const client = fakeClient(async () => ({
      parsed_output: parsedAnalysis,
      usage: { input_tokens: 1200, output_tokens: 150 },
    }));

    const result = await analyzeMealPhoto(client, sampleImage, "claude-opus-5");

    expect(result.analysis).toEqual(parsedAnalysis);
    expect(result.usage).toEqual({ inputTokens: 1200, outputTokens: 150 });
  });

  it("logs token usage for a successful analysis", async () => {
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});
    const client = fakeClient(async () => ({
      parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
      usage: { input_tokens: 900, output_tokens: 40 },
    }));

    await analyzeMealPhoto(client, sampleImage, "claude-opus-5");

    const loggedLine = logSpy.mock.calls.map((call) => call[0]).find((line) => String(line).includes("meal_photo_analyzed"));
    expect(loggedLine).toBeDefined();
    expect(loggedLine).toContain("900");
    expect(loggedLine).toContain("40");

    logSpy.mockRestore();
  });

  it("throws AnalysisError when the API call fails", async () => {
    const client = fakeClient(async () => {
      throw new Error("network error");
    });

    await expect(analyzeMealPhoto(client, sampleImage, "claude-opus-5")).rejects.toBeInstanceOf(AnalysisError);
  });

  it("throws AnalysisError when the response fails schema validation (parsed_output is null)", async () => {
    const client = fakeClient(async () => ({
      parsed_output: null,
      usage: { input_tokens: 500, output_tokens: 20 },
    }));

    await expect(analyzeMealPhoto(client, sampleImage, "claude-opus-5")).rejects.toThrow(AnalysisError);
  });
});
