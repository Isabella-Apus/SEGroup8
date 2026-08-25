import http from "k6/http";
import { check, sleep } from "k6";
import { apiBase, assertOk, defaultOptions, getNumberEnv, jsonHeaders, login, parseBody } from "./common.js";

export const options = defaultOptions();

export function setup() {
  return {
    buyerToken: login(__ENV.BUYER_USERNAME || "user", __ENV.BUYER_PASSWORD || "user123"),
    productId: getNumberEnv("NEW_PRODUCT_ID"),
    addressId: getNumberEnv("ADDRESS_ID", 1),
  };
}

export default function (data) {
  if (!data.productId) {
    throw new Error("NEW_PRODUCT_ID is required for new-order-create.k6.js");
  }

  const createRes = http.post(
    `${apiBase()}/order/create`,
    JSON.stringify({
      items: [{ productId: data.productId, quantity: Number(__ENV.QUANTITY || 1) }],
      addressId: data.addressId,
      remark: `k6 new order ${__VU}-${__ITER}`,
    }),
    jsonHeaders(data.buyerToken)
  );

  const createBody = parseBody(createRes, "create new order");
  check(createRes, {
    "create order http 200": (r) => r.status === 200,
    "create order business ok": () => createBody.code === 0,
    "create order has id": () => createBody.data && createBody.data.id,
  });

  if (__ENV.PAY_AFTER_CREATE === "true" && createBody.code === 0 && createBody.data && createBody.data.id) {
    const payRes = http.post(
      `${apiBase()}/order/${createBody.data.id}/pay`,
      JSON.stringify({ payMode: __ENV.PAY_MODE || "THIRD_PARTY", payChannel: __ENV.PAY_CHANNEL || "WECHAT" }),
      jsonHeaders(data.buyerToken)
    );
    assertOk(payRes, "pay new order");
  }

  sleep(Number(__ENV.SLEEP || 1));
}
