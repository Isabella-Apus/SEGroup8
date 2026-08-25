import http from "k6/http";
import { check, sleep } from "k6";
import { apiBase, assertOk, defaultOptions, getNumberEnv, jsonHeaders, login, parseBody } from "./common.js";

export const options = defaultOptions();

export function setup() {
  return {
    buyerToken: login(__ENV.BUYER_USERNAME || "user", __ENV.BUYER_PASSWORD || "user123"),
    auctionId: getNumberEnv("AUCTION_ID"),
    baseAmount: getNumberEnv("BID_BASE_AMOUNT", 100),
    step: getNumberEnv("BID_STEP", 5),
  };
}

export default function (data) {
  if (!data.auctionId) {
    throw new Error("AUCTION_ID is required for secondhand-auction-bid.k6.js");
  }

  const bidAmount = data.baseAmount + ((__ITER * options.vus) + __VU) * data.step;
  const bidRes = http.post(
    `${apiBase()}/secondhand/trade/auction/${data.auctionId}/bid`,
    JSON.stringify({ bidAmount }),
    jsonHeaders(data.buyerToken)
  );

  const bidBody = parseBody(bidRes, "place secondhand bid");
  check(bidRes, {
    "bid http 200": (r) => r.status === 200,
    "bid accepted or guarded": () => bidBody.code === 0 || bidBody.code === 400,
    "bid no server error": (r) => r.status < 500,
  });

  sleep(Number(__ENV.SLEEP || 1));
}
