import { createHmac } from "node:crypto";
import { expect, test, type APIRequestContext, type APIResponse } from "@playwright/test";

const baseUrl = (process.env.E2E_FINANCE_BASE_URL || "http://127.0.0.1:18085").replace(/\/$/, "");
const eventStubUrl = (process.env.E2E_FINANCE_EVENT_STUB_URL || "http://127.0.0.1:18087").replace(/\/$/, "");
const jwtSecret = process.env.JWT_SECRET || "benefits-finance-acceptance-jwt-secret-at-least-32-bytes";
const internalToken = process.env.E2E_INTERNAL_SERVICE_TOKEN ||
  "benefits-finance-acceptance-internal-token";

function base64url(value: string): string {
  return Buffer.from(value).toString("base64url");
}

function bearer(userId: number, role: "USER" | "OFFICIAL_SELLER" | "ADMIN"): string {
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64url(JSON.stringify({
    uid: userId,
    username: `acceptance-${userId}`,
    role,
    iat: now,
    exp: now + 3600,
  }));
  const signature = createHmac("sha256", jwtSecret).update(`${header}.${payload}`).digest("base64url");
  return `Bearer ${header}.${payload}.${signature}`;
}

async function json(response: APIResponse): Promise<any> {
  const text = await response.text();
  return text ? JSON.parse(text) : undefined;
}

function publicHeaders(token: string, key?: string): Record<string, string> {
  return {
    Authorization: token,
    ...(key ? { "Idempotency-Key": key } : {}),
  };
}

function internalHeaders(key?: string): Record<string, string> {
  return {
    "X-Internal-Service-Token": internalToken,
    ...(key ? { "Idempotency-Key": key } : {}),
  };
}

test("runtime, contract and security gates are enforced by the candidate image", async ({ request }) => {
  const info = await request.get(`${baseUrl}/actuator/info`);
  expect(info.status()).toBe(200);
  expect(await json(info)).toMatchObject({ app: { name: "benefits-finance-service", version: "acceptance" } });
  expect((await request.get(`${baseUrl}/actuator/health/liveness`)).status()).toBe(200);
  expect((await request.get(`${baseUrl}/actuator/health/readiness`)).status()).toBe(200);
  expect((await request.get(`${baseUrl}/actuator/flyway`)).status()).toBe(404);

  const openApiResponse = await request.get(`${baseUrl}/v3/api-docs`);
  expect(openApiResponse.status()).toBe(200);
  const openApi = await json(openApiResponse);
  const methods = new Set(["get", "post", "put", "delete", "patch"]);
  const operations = Object.values(openApi.paths || {}).reduce(
    (total: number, item: any) => total + Object.keys(item).filter((method) => methods.has(method)).length,
    0,
  );
  expect(operations).toBe(25);
  expect(openApi.paths["/internal/payments/debit"]?.post).toBeTruthy();
  expect(openApi.paths["/api/voucher/seller"]?.post).toBeTruthy();

  const unauthenticated = await request.get(`${baseUrl}/api/finance/dashboard`);
  expect(unauthenticated.status()).toBe(401);
  expect((await json(unauthenticated)).code).toBe("AUTH_REQUIRED");
  const browserJwtOnInternal = await request.get(`${baseUrl}/internal/payments/missing`, {
    headers: publicHeaders(bearer(101, "USER")),
  });
  expect(browserJwtOnInternal.status()).toBe(403);
});

test("UC21 and UC22 voucher lifecycle runs against the independent candidate", async ({ request }) => {
  const seller = bearer(7, "OFFICIAL_SELLER");
  const user = bearer(101, "USER");
  const suffix = Date.now();
  const voucher = {
    name: `acceptance-${suffix}`,
    discountType: "AMOUNT",
    discountAmount: 20,
    minAmount: 100,
    totalCount: 2,
    startTime: new Date(Date.now() - 60_000).toISOString(),
    endTime: new Date(Date.now() + 86_400_000).toISOString(),
    shopId: 88,
    scopeType: "SHOP",
  };
  const createdResponse = await request.post(`${baseUrl}/api/voucher/seller`, {
    headers: publicHeaders(seller, `uc21-create-${suffix}`), data: voucher,
  });
  expect(createdResponse.status()).toBe(201);
  const created = await json(createdResponse);

  const updated = await request.put(`${baseUrl}/api/voucher/seller/${created.id}`, {
    headers: publicHeaders(seller, `uc21-update-${suffix}`),
    data: { ...voucher, name: `updated-${suffix}` },
  });
  expect((await json(updated)).name).toBe(`updated-${suffix}`);
  expect((await request.get(`${baseUrl}/api/voucher/seller/list`, {
    headers: publicHeaders(seller),
  })).status()).toBe(200);

  expect((await request.get(`${baseUrl}/api/voucher/list`, { headers: publicHeaders(user) })).status()).toBe(200);
  const claim = await request.post(`${baseUrl}/api/voucher/${created.id}/claim`, {
    headers: publicHeaders(user, `uc22-claim-${suffix}`),
  });
  expect(claim.status()).toBe(201);
  expect((await request.get(`${baseUrl}/api/voucher/my`, { headers: publicHeaders(user) })).status()).toBe(200);

  const orderRequestId = `uc22-order-${suffix}`;
  const quoteResponse = await request.post(`${baseUrl}/internal/checkout/quote`, {
    headers: internalHeaders(orderRequestId),
    data: { orderRequestId, userId: 101, amount: 120, voucherId: created.id, shopIds: [88], productIds: [] },
  });
  const quote = await json(quoteResponse);
  expect(quote).toMatchObject({ originalAmount: 120, discountAmount: 20, payableAmount: 100 });
  const action = { orderRequestId, userId: 101, voucherId: created.id, orderId: suffix };
  const reserve = await request.post(`${baseUrl}/internal/vouchers/reserve`, {
    headers: internalHeaders(orderRequestId), data: action,
  });
  const reserveReplay = await request.post(`${baseUrl}/internal/vouchers/reserve`, {
    headers: internalHeaders(orderRequestId), data: action,
  });
  expect(await json(reserveReplay)).toEqual(await json(reserve));
  expect((await request.post(`${baseUrl}/internal/vouchers/consume`, {
    headers: internalHeaders(orderRequestId), data: action,
  })).status()).toBe(200);

  const missing = await request.post(`${baseUrl}/api/voucher/seller/999999999/close`, {
    headers: publicHeaders(seller, `missing-${suffix}`),
  });
  expect(missing.status()).toBe(404);
  expect((await json(missing)).code).toBe("VOUCHER_NOT_FOUND");
  const invalid = await request.post(`${baseUrl}/api/voucher/seller`, {
    headers: publicHeaders(seller, `invalid-${suffix}`), data: { ...voucher, totalCount: 0 },
  });
  expect(invalid.status()).toBe(400);
});

test("UC23 with UC12 and UC14 preserves idempotent money facts and strict events", async ({ request }) => {
  const user = bearer(101, "USER");
  const seller = bearer(7, "OFFICIAL_SELLER");
  const suffix = Date.now();
  const walletBefore = await json(await request.get(`${baseUrl}/api/finance/dashboard`, {
    headers: publicHeaders(user),
  }));
  const rechargeRequest = { requestId: `recharge-${suffix}`, amount: 200, channel: "WECHAT" };
  const recharge = await request.post(`${baseUrl}/api/finance/recharge`, {
    headers: publicHeaders(user, rechargeRequest.requestId), data: rechargeRequest,
  });
  const rechargeResult = await json(recharge);
  const rechargeReplay = await request.post(`${baseUrl}/api/finance/recharge`, {
    headers: publicHeaders(user, rechargeRequest.requestId), data: rechargeRequest,
  });
  expect(await json(rechargeReplay)).toEqual(rechargeResult);
  const reused = await request.post(`${baseUrl}/api/finance/recharge`, {
    headers: publicHeaders(user, rechargeRequest.requestId), data: { ...rechargeRequest, amount: 201 },
  });
  expect(reused.status()).toBe(409);
  expect((await json(reused)).code).toBe("IDEMPOTENCY_KEY_REUSED");

  const orderId = suffix;
  const debitRequest = { paymentRequestId: `payment-${suffix}`, orderId, userId: 101, amount: 60 };
  const debit = await request.post(`${baseUrl}/internal/payments/debit`, {
    headers: internalHeaders(debitRequest.paymentRequestId), data: debitRequest,
  });
  const debitResult = await json(debit);
  const debitReplay = await request.post(`${baseUrl}/internal/payments/debit`, {
    headers: internalHeaders(debitRequest.paymentRequestId), data: debitRequest,
  });
  expect(await json(debitReplay)).toEqual(debitResult);
  expect((await request.get(`${baseUrl}/internal/payments/${debitRequest.paymentRequestId}`, {
    headers: internalHeaders(),
  })).status()).toBe(200);

  const refundRequest = {
    refundRequestId: `refund-${suffix}`,
    paymentRequestId: debitRequest.paymentRequestId,
    orderId,
    userId: 101,
    amount: 20,
  };
  expect((await request.post(`${baseUrl}/internal/payments/refund`, {
    headers: internalHeaders(refundRequest.refundRequestId), data: refundRequest,
  })).status()).toBe(200);
  const settlementRequest = { orderId, sellerId: 7, amount: 40 };
  const settlement = await request.post(`${baseUrl}/internal/settlements`, {
    headers: internalHeaders(`settlement-${orderId}-7`), data: settlementRequest,
  });
  const settlementReplay = await request.post(`${baseUrl}/internal/settlements`, {
    headers: internalHeaders(`settlement-${orderId}-7`), data: settlementRequest,
  });
  expect(await json(settlementReplay)).toEqual(await json(settlement));

  const wallet = await json(await request.get(`${baseUrl}/api/finance/dashboard`, {
    headers: publicHeaders(user),
  }));
  expect(Number(wallet.personalBalance)).toBeCloseTo(
    Number(walletBefore.personalBalance) + 160,
    2,
  );
  const business = await json(await request.get(`${baseUrl}/api/finance/business/records`, {
    headers: publicHeaders(seller),
  }));
  expect(business).toContainEqual(expect.objectContaining({ orderId, tradeType: "SETTLEMENT", amount: 40 }));

  await expect.poll(async () => {
    const response = await request.get(`${eventStubUrl}/__received`);
    if (!response.ok()) return [];
    return (await json(response)).map((event: any) => event.eventType).sort();
  // CI starts the relay, MySQL and the strict HTTP sink on the same runner.
  // Keep the assertion bounded, but allow a cold runner more than one relay
  // retry window before declaring the delivery contract broken.
  }, { timeout: 60_000 }).toEqual(expect.arrayContaining([
    "PaymentCompleted.v1", "PaymentCompleted.v1", "PaymentCompleted.v1", "RefundCompleted.v1",
  ]));
});
