import { check, fail } from "k6";
import http from "k6/http";
import { Rate } from "k6/metrics";

export const businessSuccess = new Rate("business_success");
export const businessGuarded = new Rate("business_guarded");
export const serverError = new Rate("server_error");

export function apiBase() {
  return (__ENV.BASE_URL || "http://host.docker.internal:8089/api").replace(/\/$/, "");
}

export function defaultOptions() {
  return {
    vus: numberEnv("VUS", 2),
    duration: __ENV.DURATION || "10s",
    thresholds: {
      http_req_failed: ["rate<0.05"],
      http_req_duration: ["p(95)<1500"],
      server_error: ["rate==0"],
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

export function parseBody(response, label) {
  try {
    return JSON.parse(response.body || "{}");
  } catch (error) {
    fail(`${label} returned non-JSON data: ${response.body}`);
  }
}

export function observeResponse(response, body, acceptedCodes = [0]) {
  const codeAccepted = acceptedCodes.includes(body.code);
  businessSuccess.add(body.code === 0);
  businessGuarded.add(codeAccepted && body.code !== 0);
  serverError.add(response.status >= 500 || (typeof body.code === "number" && body.code >= 500));
  return codeAccepted;
}

export function requireSuccess(response, label) {
  const body = parseBody(response, label);
  const ok = check(response, {
    [`${label}: HTTP 200`]: (value) => value.status === 200,
    [`${label}: business code 0`]: () => body.code === 0,
  });
  observeResponse(response, body, [0]);
  if (!ok) {
    fail(`${label} failed: status=${response.status}, body=${response.body}`);
  }
  return body.data;
}

export function login(username, password) {
  if (!username || !password) {
    fail("username and password are required");
  }
  const response = http.post(
    `${apiBase()}/auth/login`,
    JSON.stringify({ username, password }),
    jsonHeaders()
  );
  const data = requireSuccess(response, `login ${username}`);
  if (!data || !data.token) {
    fail(`login ${username} returned no token`);
  }
  return data.token;
}

export function numberEnv(name, fallback = undefined) {
  const raw = __ENV[name];
  if (raw === undefined || raw === "") {
    return fallback;
  }
  const parsed = Number(raw);
  if (!Number.isFinite(parsed)) {
    fail(`${name} must be numeric`);
  }
  return parsed;
}

export function query(params) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join("&");
}
