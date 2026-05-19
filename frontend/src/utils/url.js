export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

export const ASSET_BASE_URL = import.meta.env.VITE_ASSET_BASE_URL || "";

export function toAssetUrl(url) {
    if (!url) {
        return "";
    }
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
        return url;
    }
    const normalized = url.startsWith("/") ? url : `/${url}`;
    if (!ASSET_BASE_URL) {
        return normalized;
    }
    return `${ASSET_BASE_URL.replace(/\/$/, "")}${normalized}`;
}
