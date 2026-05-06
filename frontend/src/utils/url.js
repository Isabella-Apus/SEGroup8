export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

export function toApiAssetUrl(url) {
    if (!url) {
        return "";
    }
    const normalizedUrl = String(url).replace(/\\/g, "/");
    const legacyLocalOrigin = normalizedUrl.match(/^https?:\/\/(?:localhost|127\.0\.0\.1):8080(\/.*)?$/i);
    if (legacyLocalOrigin) {
        return encodeURI(legacyLocalOrigin[1] || "");
    }
    if (/^(https?:)?\/\//i.test(normalizedUrl) || /^(data|blob):/i.test(normalizedUrl)) {
        return encodeURI(normalizedUrl);
    }
    let path = normalizedUrl;
    if (!path.startsWith("/")) {
        path = path.startsWith("uploads/") ? `/${path}` : `/uploads/${path}`;
    }
    return encodeURI(path);
}

export function buildRealtimeWsUrl() {
    const explicit = import.meta.env.VITE_WS_BASE_URL;
    if (explicit) {
        const base = String(explicit).replace(/\/$/, "");
        if (/^wss?:\/\//i.test(base)) {
            return `${base}/ws/realtime`;
        }
        return `${window.location.origin.replace(/^http/, "ws")}${base.startsWith("/") ? base : `/${base}`}/ws/realtime`;
    }
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${window.location.host}/ws/realtime`;
}
