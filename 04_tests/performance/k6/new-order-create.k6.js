import { check, sleep } from "k6";
import http from "k6/http";
import {
  apiBase,
  defaultOptions,
  jsonHeaders,
  login,
  numberEnv,
  observeResponse,
  parseBody,
} from "./common.js";

export const options = defaultOptions();

export function setup() {
  return {
    buyerToken: login(
      __ENV.BUYER_USERNAME || "perf_buyer",
      __ENV.BUYER_PASSWORD || "perf_buyer_2026"
    ),
    productId: numberEnv("NEW_PRODUCT_ID", 980001),
    addressId: numberEnv("ADDRESS_ID", 970001),
  };
}

export default function (data) {
  const response = http.post(
    `${apiBase()}/order/create`,
    JSON.stringify({
      items: [{ productId: data.productId, quantity: Number(__ENV.QUANTITY || 1) }],
      addressId: data.addressId,
      remark: `k6 monolith baseline ${__VU}-${__ITER}`,
    }),
    { ...jsonHeaders(data.buyerToken), tags: { endpoint: "new-order-create" } }
  );
  const body = parseBody(response, "create new order");
  const accepted = observeResponse(response, body, [0, 400]);
  check(response, {
    "new order: no server error": (value) => value.status < 500,
    "new order: accepted or business guarded": () => accepted,
  });
  sleep(Number(__ENV.SLEEP || 0.2));
}
