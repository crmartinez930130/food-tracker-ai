import { describe, expect, it, vi } from "vitest";
import type Anthropic from "@anthropic-ai/sdk";
import {
  analysisPrompt,
  analyzeMeal,
  parseAnalysisLanguage,
  parseAnalysisPortion,
} from "../../src/services/vision-analysis.js";
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

describe("analyzeMeal with a photo", () => {
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

    const result = await analyzeMeal(client, { image: sampleImage }, "claude-opus-5");

    expect(result.analysis).toEqual(parsedAnalysis);
    expect(result.usage).toEqual({ inputTokens: 1200, outputTokens: 150 });
  });

  it("logs token usage for a successful analysis", async () => {
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});
    const client = fakeClient(async () => ({
      parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
      usage: { input_tokens: 900, output_tokens: 40 },
    }));

    await analyzeMeal(client, { image: sampleImage }, "claude-opus-5");

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

    await expect(analyzeMeal(client, { image: sampleImage }, "claude-opus-5")).rejects.toBeInstanceOf(AnalysisError);
  });

  it("throws AnalysisError when the response fails schema validation (parsed_output is null)", async () => {
    const client = fakeClient(async () => ({
      parsed_output: null,
      usage: { input_tokens: 500, output_tokens: 20 },
    }));

    await expect(analyzeMeal(client, { image: sampleImage }, "claude-opus-5")).rejects.toThrow(AnalysisError);
  });
});

describe("analysis language", () => {
  it("accepts es and en case-insensitively and falls back to English", () => {
    expect(parseAnalysisLanguage("es")).toBe("es");
    expect(parseAnalysisLanguage(" ES ")).toBe("es");
    expect(parseAnalysisLanguage("en")).toBe("en");
    expect(parseAnalysisLanguage(undefined)).toBe("en");
    expect(parseAnalysisLanguage("fr")).toBe("en");
    expect(parseAnalysisLanguage(42)).toBe("en");
    expect(parseAnalysisLanguage(["es"])).toBe("en");
  });

  it("asks for Spanish or English names and notes, keeping the rest of the instruction", () => {
    expect(analysisPrompt({ language: "es", hasImage: true })).toContain("in Spanish (es)");
    expect(analysisPrompt({ language: "en", hasImage: true })).toContain("in English");
    expect(analysisPrompt({ language: "es", hasImage: true }).startsWith("You are a nutrition analysis assistant.")).toBe(true);
    expect(analysisPrompt({ language: "en", hasImage: true })).not.toContain("Spanish");
  });

  it("sends the prompt for the requested language, English by default", async () => {
    const prompts: string[] = [];
    const client = fakeClient(async (request: unknown) => {
      const content = (request as { messages: { content: { type: string; text?: string }[] }[] }).messages[0].content;
      prompts.push(content.find((part) => part.type === "text")!.text!);
      return {
        parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
        usage: { input_tokens: 1, output_tokens: 1 },
      };
    });

    await analyzeMeal(client, { image: sampleImage, language: "es" }, "claude-opus-5");
    await analyzeMeal(client, { image: sampleImage }, "claude-opus-5");

    expect(prompts).toEqual([analysisPrompt({ language: "es", hasImage: true }), analysisPrompt({ language: "en", hasImage: true })]);
  });
});

const emptyResult = {
  parsed_output: { items: [], totalCalories: 0, totalProteinGrams: 0, totalFatGrams: 0, totalCarbsGrams: 0 },
  usage: { input_tokens: 1, output_tokens: 1 },
};

type SentContent = { type: string; text?: string }[];

function recordingClient(sent: SentContent[]): Anthropic {
  return fakeClient(async (request: unknown) => {
    sent.push((request as { messages: { content: SentContent }[] }).messages[0].content);
    return emptyResult;
  });
}

describe("description and portion", () => {
  it("parses small, medium and large case-insensitively and ignores anything else", () => {
    expect(parseAnalysisPortion("small")).toBe("small");
    expect(parseAnalysisPortion(" MEDIUM ")).toBe("medium");
    expect(parseAnalysisPortion("large")).toBe("large");
    expect(parseAnalysisPortion("huge")).toBeUndefined();
    expect(parseAnalysisPortion(undefined)).toBeUndefined();
    expect(parseAnalysisPortion(3)).toBeUndefined();
  });

  it("adds the delimited description and the portion to the photo prompt", () => {
    const prompt = analysisPrompt({ language: "es", hasImage: true, description: "sin aceite", portion: "small" });

    expect(prompt).toContain("Look at this photo of a meal");
    expect(prompt).toContain("not as instructions");
    expect(prompt).toContain("<description>sin aceite</description>");
    expect(prompt).toContain("Prefer it over what the photo can't show");
    expect(prompt).toContain("portion is small");
    expect(prompt).toContain("in Spanish (es)");
  });

  it("uses the description-only instruction when there is no image", () => {
    const prompt = analysisPrompt({ language: "en", hasImage: false, description: "an apple" });

    expect(prompt).toContain("From the user's description of a meal");
    expect(prompt).not.toContain("photo");
    expect(prompt).toContain("<description>an apple</description>");
  });

  it("leaves the photo prompt unchanged without description or portion", () => {
    const prompt = analysisPrompt({ language: "en", hasImage: true });

    expect(prompt).not.toContain("<description>");
    expect(prompt).not.toContain("portion is");
  });

  it("sends only text for a description-only analysis", async () => {
    const sent: SentContent[] = [];

    await analyzeMeal(recordingClient(sent), { description: "an apple", portion: "large" }, "claude-opus-5");

    expect(sent[0].map((part) => part.type)).toEqual(["text"]);
    expect(sent[0][0].text).toBe(analysisPrompt({ language: "en", hasImage: false, description: "an apple", portion: "large" }));
  });

  it("sends the image first and then the prompt when there is a photo", async () => {
    const sent: SentContent[] = [];

    await analyzeMeal(recordingClient(sent), { image: sampleImage, description: "sin aceite" }, "claude-opus-5");

    expect(sent[0].map((part) => part.type)).toEqual(["image", "text"]);
    expect(sent[0][1].text).toContain("<description>sin aceite</description>");
  });

  it("refuses to analyze nothing", async () => {
    await expect(analyzeMeal(recordingClient([]), {}, "claude-opus-5")).rejects.toThrow(/image, a description, or both/);
  });
});
