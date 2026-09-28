import sharp from "sharp";

export const MAX_LONG_EDGE_PX = 1568;
export const JPEG_QUALITY = 80;

export interface PreprocessedImage {
  buffer: Buffer;
  mediaType: "image/jpeg";
  width: number;
  height: number;
}

export async function preprocessImage(input: Buffer): Promise<PreprocessedImage> {
  const pipeline = sharp(input).resize({
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
