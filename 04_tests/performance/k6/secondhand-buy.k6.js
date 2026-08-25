import http from "k6/http";
import { check, sleep } from "k6";
import { apiBase, defaultOptions, getNumberEnv, jsonHeaders, login, parseBody } from "./common.js";

export const options = defaultOptions();

export function setup() {
  const ids = (__ENV.SECONDHAND_PRODUCT_IDS || "")
    .split(",")
    .map((id) => Number(id.trim()))
    .filter((id) => !Number.isNaN(id));
  return {
    buyerToken: login(__ENV.BUYER_USERNAME || "user", __ENV.BUYER_PASSWORD || "user123"),
    productIds: ids,
    addressId: getNumberEnv("ADDRESS_ID", 1),
  };
}

export default function (data) {
  if (!data.productIds.length) {
    throw new Error("SECONDHAND_PRODUCT_IDS is required for secondhand-buy.k6.js");
  }

  const productId = data.productIds[(__VU + __ITER) % data.productIds.length];
  const buyRes = http.post(
    `${apiBase()}/secondhand/${productId}/buy`,
    JSON.stringify({
      addressId: data.addressId,
      remark: `k6 secondhand buy ${__VU}-${__ITER}`,
    }),
    jsonHeaders(data.buyerToken)
  );

  const buyBody = parseBody(buyRes, "buy secondhand product");
  check(buyRes, {
    "secondhand buy http 200": (r) => r.status === 200,
    "secondhand buy accepted or guarded": () => buyBody.code === 0 || buyBody.code === 400,
    "secondhand buy no server error": (r) => r.status < 500,
  });

  sleep(Number(__ENV.SLEEP || 1));
}
