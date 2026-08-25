import http from "k6/http";
import { check, fail } from "k6";

export function apiBase() {
  return (__ENV.BASE_URL || "http://127.0.0.1:8080/api").replace(/\/$/, "");
}

export function defaultOptions() {
  return {
    vus: Number(__ENV.VUS || 10),
    duration: __ENV.DURATION || "30s",
    thresholds: {
      http_req_failed: ["rate<0.05"],
      http_req_duration: ["p(95)<1200"],
    },
  };
}

export function jsonHeaders(token) {
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

export function parseBody(res, label) {
  try {
    return JSON.parse(res.body || "{}");
  } catch (error) {
    fail(`${label} returned non-JSON response: ${res.body}`);
  }
}

export function assertOk(res, label) {
  const body = parseBody(res, label);
  const ok = check(res, {
    [`${label} http 200`]: (r) => r.status === 200,
    [`${label} business code 0`]: () => body.code === 0,
  });
  if (!ok) {
    fail(`${label} failed: status=${res.status}, body=${res.body}`);
  }
  return body.data;
}

export function login(username, password) {
  if (!username || !password) {
    fail("username and password are required for authenticated k6 scenario");
  }
  const res = http.post(
    `${apiBase()}/auth/login`,
    JSON.stringify({ username, password }),
    jsonHeaders()
  );
  const data = assertOk(res, `login ${username}`);
  if (!data || !data.token) {
    fail(`login ${username} did not return token`);
  }
  return data.token;
}

export function getNumberEnv(name, fallback = undefined) {
  const value = __ENV[name];
  if (value === undefined || value === "") {
    return fallback;
  }
  const parsed = Number(value);
  if (Number.isNaN(parsed)) {
    fail(`${name} must be a number`);
  }
  return parsed;
}

export function query(params) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join("&");
}
