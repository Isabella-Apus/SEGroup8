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
  const firstId = numberEnv("SECONDHAND_FIRST_ID", 990001);
  const count = numberEnv("SECONDHAND_PRODUCT_COUNT", 40);
  return {
    buyerToken: login(
      __ENV.BUYER_USERNAME || "perf_buyer",
      __ENV.BUYER_PASSWORD || "perf_buyer_2026"
    ),
    firstId,
    count,
    addressId: numberEnv("ADDRESS_ID", 970001),
  };
}

export default function (data) {
  const offset = ((__VU - 1) + __ITER * options.vus) % data.count;
  const productId = data.firstId + offset;
  const response = http.post(
    `${apiBase()}/secondhand/${productId}/buy`,
    JSON.stringify({
      addressId: data.addressId,
      remark: `k6 secondhand baseline ${__VU}-${__ITER}`,
    }),
    { ...jsonHeaders(data.buyerToken), tags: { endpoint: "secondhand-buy" } }
  );
  const body = parseBody(response, "buy secondhand product");
  const accepted = observeResponse(response, body, [0, 400, 404]);
  check(response, {
    "secondhand buy: no server error": (value) => value.status < 500,
    "secondhand buy: accepted or business guarded": () => accepted,
  });
  sleep(Number(__ENV.SLEEP || 0.2));
}
