import { getToken } from "@/utils/storage";

const EVENT_BUS_NAME = "segroup8-realtime-event";
let socket = null;
let reconnectTimer = null;
let manuallyClosed = false;

function buildWsUrl() {
  const explicit = import.meta.env.VITE_WS_BASE_URL;
  if (explicit) {
    return `${String(explicit).replace(/\/$/, "")}/ws/realtime`;
  }
  const isLocal = ["127.0.0.1", "localhost"].includes(window.location.hostname);
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
  const url = buildWsUrl();
  if (!url) return;
  const token = getToken();
  if (!token) return;
  socket = new WebSocket(`${url}?token=${encodeURIComponent(token)}`);
  socket.onopen = () => {
    // query token auth
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
      reconnectTimer = setTimeout(() => connect(), 2000);
    }
  };
}

export function startRealtimeClient() {
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
