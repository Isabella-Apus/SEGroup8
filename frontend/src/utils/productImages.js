import { toApiAssetUrl } from "@/utils/url";

function normalizeImageList(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  if (typeof value !== "string") return [];

  const text = value.trim();
  if (!text) return [];

  if (text.startsWith("[") && text.endsWith("]")) {
    try {
      const parsed = JSON.parse(text);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  return text.split(",").map((item) => item.trim()).filter(Boolean);
}

export function getProductImages(product) {
  const raw = [
    ...normalizeImageList(product?.images),
    ...normalizeImageList(product?.imageUrls),
  ];
  const images = raw
    .concat(product?.imageUrl ? [product.imageUrl] : [])
    .concat(product?.cover ? [product.cover] : [])
    .filter(Boolean)
    .map((url) => String(url).trim())
    .filter(Boolean);
  return Array.from(new Set(images));
}

export function getFirstProductImage(product) {
  return getProductImages(product)[0] || "";
}

export function toFullImageUrl(url) {
  if (!url) return "";
  return toApiAssetUrl(url);
}

export function toFullProductImageUrls(product) {
  return getProductImages(product).map(toFullImageUrl).filter(Boolean);
}
