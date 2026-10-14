import { describe, expect, it } from "vitest";
import sharp from "sharp";
import { MAX_LONG_EDGE_PX, detectImageType, preprocessImage } from "../../src/services/image-preprocessing.js";

async function makeSampleImage(width: number, height: number): Promise<Buffer> {
  return sharp({
    create: {
      width,
      height,
      channels: 3,
      background: { r: 200, g: 120, b: 60 },
    },
  })
    .png()
    .toBuffer();
}

describe("preprocessImage", () => {
  it("downscales an oversized image so the long edge is capped", async () => {
    const oversized = await makeSampleImage(3000, 2000);
    const result = await preprocessImage(oversized);

    expect(Math.max(result.width, result.height)).toBeLessThanOrEqual(MAX_LONG_EDGE_PX);
    expect(result.mediaType).toBe("image/jpeg");
  });

  it("re-encodes as JPEG regardless of input format", async () => {
    const png = await makeSampleImage(400, 300);
    const result = await preprocessImage(png);

    const metadata = await sharp(result.buffer).metadata();
    expect(metadata.format).toBe("jpeg");
  });

  it("does not upscale an image already smaller than the cap", async () => {
    const small = await makeSampleImage(300, 200);
    const result = await preprocessImage(small);

    expect(result.width).toBe(300);
    expect(result.height).toBe(200);
  });

  it("applies the EXIF orientation so a rotated photo comes out upright", async () => {
    // Pixels stored 400x300 (landscape) with orientation 6 = "rotate 90° clockwise to display",
    // which is what phone cameras write for a portrait shot.
    const stored = await sharp(await makeSampleImage(400, 300)).jpeg().withMetadata({ orientation: 6 }).toBuffer();

    const result = await preprocessImage(stored);

    expect(result.width).toBe(300);
    expect(result.height).toBe(400);
    expect((await sharp(result.buffer).metadata()).orientation).toBeUndefined();
  });

  it("leaves a photo without an orientation tag in its stored orientation", async () => {
    const untagged = await sharp(await makeSampleImage(400, 300)).jpeg().toBuffer();

    const result = await preprocessImage(untagged);

    expect(result.width).toBe(400);
    expect(result.height).toBe(300);
  });
});

describe("detectImageType", () => {
  it("recognizes JPEG and PNG by their first bytes", async () => {
    const png = await makeSampleImage(10, 10);
    const jpeg = await sharp(png).jpeg().toBuffer();

    expect(detectImageType(png)).toBe("png");
    expect(detectImageType(jpeg)).toBe("jpeg");
  });

  it("rejects other formats whatever they claim to be", () => {
    const svg = Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"/>');
    // An ISO-BMFF box: size, "ftyp", brand "heic".
    const heif = Buffer.from([0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63]);
    const gif = Buffer.from("GIF89a");

    expect(detectImageType(svg)).toBeNull();
    expect(detectImageType(heif)).toBeNull();
    expect(detectImageType(gif)).toBeNull();
    expect(detectImageType(Buffer.alloc(0))).toBeNull();
    expect(detectImageType(Buffer.from([0xff, 0xd8]))).toBeNull();
  });
});
