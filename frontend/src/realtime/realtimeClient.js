import { getToken } from "@/utils/storage";

const EVENT_BUS_NAME = "segroup8-realtime-event";
let socket = null;
let reconnectTimer = null;
let manuallyClosed = false;
let reconnectAttempts = 0;
let heartbeatTimer = null;

function isRealtimeEnabled() {
    const explicit = String(import.meta.env.VITE_ENABLE_REALTIME || "").toLowerCase();
    if (explicit === "false" || explicit === "0" || explicit === "off") {
        return false;
    }
    if (explicit === "true" || explicit === "1" || explicit === "on") {
        return true;
    }
    const dataSource = String(import.meta.env.VITE_DATA_SOURCE || "").toLowerCase();
    return dataSource !== "mock";
}

function buildWsUrl() {
    const explicit = import.meta.env.VITE_WS_BASE_URL;
    if (explicit) {
        const normalized = String(explicit).replace(/\/$/, "");
        return normalized.endsWith("/ws/realtime") ? normalized : `${normalized}/ws/realtime`;
    }
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${window.location.host}/ws/realtime`;
}

function emitRealtimeEvent(message) {
    window.dispatchEvent(new CustomEvent(EVENT_BUS_NAME, { detail: message }));
}

function clearHeartbeat() {
    if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
    }
}

function scheduleReconnect() {
    if (manuallyClosed) {
        return;
    }
    reconnectAttempts += 1;
    const delay = Math.min(1000 * Math.max(reconnectAttempts, 1), 5000);
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
    }
    reconnectTimer = setTimeout(() => connect(), delay);
}

function connect() {
    if (!isRealtimeEnabled()) return;
    const url = buildWsUrl();
    const token = getToken();
    if (!url || !token) return;
    clearHeartbeat();
    socket = new WebSocket(`${url}?token=${encodeURIComponent(token)}`);
    socket.onopen = () => {
        const reconnected = reconnectAttempts > 0;
        reconnectAttempts = 0;
        clearHeartbeat();
        heartbeatTimer = setInterval(() => {
            sendRealtimeMessage({ eventType: "PING", payload: { ts: Date.now() } });
        }, 20000);
        if (reconnected) {
            emitRealtimeEvent({ eventType: "REALTIME_RECONNECTED", payload: { ok: true } });
        }
    };
    socket.onerror = () => {
        if (socket && socket.readyState === WebSocket.OPEN) {
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
        clearHeartbeat();
        scheduleReconnect();
    };
}

export function startRealtimeClient() {
    if (!isRealtimeEnabled()) return;
    manuallyClosed = false;
    if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) {
        return;
    }
    connect();
}

export function stopRealtimeClient() {
    manuallyClosed = true;
    reconnectAttempts = 0;
    clearHeartbeat();
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

export function sendRealtimeMessage(payload) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        return false;
    }
    socket.send(JSON.stringify(payload));
    return true;
}
