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
  const buyerTokens = [
    login(
      __ENV.BUYER_USERNAME || "perf_buyer",
      __ENV.BUYER_PASSWORD || "perf_buyer_2026"
    ),
    login(
      __ENV.BUYER_USERNAME_2 || "perf_buyer_2",
      __ENV.BUYER_PASSWORD_2 || "perf_buyer_2_2026"
    ),
  ];
  return {
    buyerTokens,
    firstAuctionId: numberEnv("AUCTION_FIRST_ID", 999001),
    auctionCount: numberEnv("AUCTION_COUNT", 2),
    baseAmount: numberEnv("BID_BASE_AMOUNT", 1000),
    step: numberEnv("BID_STEP", 5),
  };
}

export default function (data) {
  const slot = (__VU - 1) % data.auctionCount;
  const auctionId = data.firstAuctionId + slot;
  const buyerToken = data.buyerTokens[slot % data.buyerTokens.length];
  const bidAmount = data.baseAmount + (__ITER * options.vus + __VU) * data.step;
  const response = http.post(
    `${apiBase()}/secondhand/trade/auction/${auctionId}/bid`,
    JSON.stringify({ bidAmount }),
    { ...jsonHeaders(buyerToken), tags: { endpoint: "secondhand-auction-bid" } }
  );
  const body = parseBody(response, "place secondhand bid");
  const accepted = observeResponse(response, body, [0, 400]);
  check(response, {
    "auction bid: no server error": (value) => value.status < 500,
    "auction bid: accepted or concurrency guarded": () => accepted,
  });
  sleep(Number(__ENV.SLEEP || 0.2));
}
