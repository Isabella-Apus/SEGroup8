import { check, fail, sleep } from "k6";
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

const defaults = defaultOptions();
let failureSamples = 0;
export const options = {
  ...defaults,
  thresholds: {
    ...defaults.thresholds,
    business_success: ["rate>0.99"],
    business_guarded: ["rate<0.01"],
    checks: ["rate>0.99"],
  },
};

export function setup() {
  let dedicatedTokenCount = 0;
  if (__ENV.BUYER_TOKENS) {
    const dedicatedTokens = __ENV.BUYER_TOKENS.split(";").filter(Boolean);
    dedicatedTokenCount = dedicatedTokens.length;
    if (dedicatedTokenCount < options.vus * 2) {
      fail(`BUYER_TOKENS needs two dedicated buyers per VU; expected ${options.vus * 2}, got ${dedicatedTokenCount}`);
    }
  }
  const suppliedTokens = [__ENV.BUYER_TOKEN, __ENV.BUYER_TOKEN_2];
  if (suppliedTokens.filter(Boolean).length === 1) {
    fail("BUYER_TOKEN and BUYER_TOKEN_2 must be supplied together");
  }
  const buyerTokens = dedicatedTokenCount > 0 || suppliedTokens.every(Boolean)
    ? null
    : [
      login(
        __ENV.BUYER_USERNAME || "perf_buyer",
        __ENV.BUYER_PASSWORD || "perf_buyer_2026"
      ),
      login(
        __ENV.BUYER_USERNAME_2 || "perf_buyer_2",
        __ENV.BUYER_PASSWORD_2 || "perf_buyer_2_2026"
      ),
    ];
  const auctionCount = numberEnv("AUCTION_COUNT", 2);
  if (auctionCount < options.vus) {
    fail(`AUCTION_COUNT (${auctionCount}) must be at least VUS (${options.vus})`);
  }
  return {
    buyerTokens,
    dedicatedTokenCount,
    firstAuctionId: numberEnv("AUCTION_FIRST_ID", 999001),
    auctionCount,
    baseAmount: numberEnv("BID_BASE_AMOUNT", 1000),
    step: numberEnv("BID_STEP", 5),
  };
}

export default function (data) {
  const slot = (__VU - 1) % data.auctionCount;
  const auctionId = data.firstAuctionId + slot;
  let buyerToken;
  if (data.dedicatedTokenCount > 0) {
    const dedicatedTokens = __ENV.BUYER_TOKENS.split(";");
    buyerToken = dedicatedTokens[slot * 2 + (__ITER % 2)];
  } else {
    const buyerTokens = data.buyerTokens || [__ENV.BUYER_TOKEN, __ENV.BUYER_TOKEN_2];
    buyerToken = buyerTokens[(__ITER + slot) % buyerTokens.length];
  }
  const bidAmount = data.baseAmount + (__ITER * options.vus + __VU) * data.step;
  const response = http.post(
    `${apiBase()}/secondhand/trade/auction/${auctionId}/bid`,
    JSON.stringify({ bidAmount }),
    { ...jsonHeaders(buyerToken), tags: { endpoint: "secondhand-auction-bid" } }
  );
  const body = parseBody(response, "place secondhand bid");
  if ((response.status >= 500 || body.code !== 0) && failureSamples < 2) {
    console.warn(`auction bid failure sample: status=${response.status}, body=${response.body}`);
    failureSamples += 1;
  }
  const accepted = observeResponse(response, body, [0, 400, 409]);
  check(response, {
    "auction bid: no server error": (value) => value.status < 500,
    "auction bid: business success": () => accepted && body.code === 0,
  });
  sleep(Number(__ENV.SLEEP || 0.2));
}
