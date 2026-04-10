import { getToken } from "@/utils/storage";

const EVENT_BUS_NAME = "segroup8-realtime-event";
const RT_DISABLED_SESSION_KEY = "segroup8_realtime_disabled";
let socket = null;
let reconnectTimer = null;
let manuallyClosed = false;
let reconnectAttempts = 0;
let warnedUnavailable = false;

function isRealtimeEnabled() {
    if (window.sessionStorage.getItem(RT_DISABLED_SESSION_KEY) === "1") {
        return false;
    }
    const explicit = String(
        import.meta.env.VITE_ENABLE_REALTIME || "",
    ).toLowerCase();
    if (import.meta.env.DEV && !explicit) {
        return false;
    }
    if (explicit === "false" || explicit === "0" || explicit === "off") {
        return false;
    }
    if (explicit === "true" || explicit === "1" || explicit === "on") {
        return true;
    }
    const dataSource = String(
        import.meta.env.VITE_DATA_SOURCE || "",
    ).toLowerCase();
    return dataSource !== "mock";
}

function buildWsUrl() {
    const explicit = import.meta.env.VITE_WS_BASE_URL;
    if (explicit) {
        return `${String(explicit).replace(/\/$/, "")}/ws/realtime`;
    }
    const isLocal = ["127.0.0.1", "localhost"].includes(
        window.location.hostname,
    );
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    if (isLocal) {
        return `${protocol}://127.0.0.1:8080/ws/realtime`;
    }
    return `${protocol}://${window.location.host}/ws/realtime`;
}

function emitRealtimeEvent(message) {
    window.dispatchEvent(new CustomEvent(EVENT_BUS_NAME, { detail: message }));
}

function connect() {
    if (!isRealtimeEnabled()) return;
    const url = buildWsUrl();
    if (!url) return;
    const token = getToken();
    if (!token) return;
    socket = new WebSocket(`${url}?token=${encodeURIComponent(token)}`);
    socket.onopen = () => {
        reconnectAttempts = 0;
        warnedUnavailable = false;
    };
    socket.onerror = () => {
        if (!warnedUnavailable) {
            warnedUnavailable = true;
            console.warn(
                "realtime websocket unavailable, switched to polling-only pages",
            );
        }
        window.sessionStorage.setItem(RT_DISABLED_SESSION_KEY, "1");
        manuallyClosed = true;
        if (socket) {
            socket.close();
        }
    };
    socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            emitRealtimeEvent(data);
        } catch (_) {
            // ignore malformed payload
        }
    };
    socket.onclose = () => {
        socket = null;
        if (!manuallyClosed) {
            reconnectAttempts += 1;
            if (reconnectAttempts <= 3) {
                reconnectTimer = setTimeout(() => connect(), 2000);
            }
        }
    };
}

export function startRealtimeClient() {
    if (!isRealtimeEnabled()) return;
    manuallyClosed = false;
    if (socket) return;
    connect();
}

export function stopRealtimeClient() {
    manuallyClosed = true;
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }
    if (socket) {
        socket.close();
        socket = null;
    }
}

export function onRealtimeEvent(handler) {
    window.addEventListener(EVENT_BUS_NAME, handler);
    return () => window.removeEventListener(EVENT_BUS_NAME, handler);
}
