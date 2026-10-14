import sharp from "sharp";

export const MAX_LONG_EDGE_PX = 1568;
export const JPEG_QUALITY = 80;

export interface PreprocessedImage {
  buffer: Buffer;
  mediaType: "image/jpeg";
  width: number;
  height: number;
}

/** Largest input decoded, in pixels: ample for phone photos, and a cap on what a crafted file can demand. */
export const MAX_INPUT_PIXELS = 24_000_000;

const JPEG_SIGNATURE = [0xff, 0xd8, 0xff];
const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

const startsWith = (buffer: Buffer, signature: number[]) =>
  buffer.length >= signature.length && signature.every((byte, index) => buffer[index] === byte);

/**
 * The image format by its first bytes, or null when it's neither JPEG nor PNG. The declared
 * mimetype comes from the client, so this is what keeps other decoders (SVG, HEIF...) out of reach.
 */
export function detectImageType(buffer: Buffer): "jpeg" | "png" | null {
  if (startsWith(buffer, JPEG_SIGNATURE)) return "jpeg";
  if (startsWith(buffer, PNG_SIGNATURE)) return "png";
  return null;
}

export async function preprocessImage(input: Buffer): Promise<PreprocessedImage> {
  // rotate() with no angle applies the EXIF orientation (and drops the tag): phone photos are
  // often stored sideways with only a tag saying how to display them.
  const pipeline = sharp(input, { limitInputPixels: MAX_INPUT_PIXELS, failOn: "error" }).rotate().resize({
    width: MAX_LONG_EDGE_PX,
    height: MAX_LONG_EDGE_PX,
    fit: "inside",
    withoutEnlargement: true,
  });

  const buffer = await pipeline.jpeg({ quality: JPEG_QUALITY }).toBuffer();
  const { width, height } = await sharp(buffer).metadata();

  return {
    buffer,
    mediaType: "image/jpeg",
    width: width ?? 0,
    height: height ?? 0,
  };
}
