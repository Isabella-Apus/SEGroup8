import { expect, test, type APIRequestContext } from "@playwright/test";

const user = { "X-User-Id": "1", "X-User-Role": "USER" };
const seller = { "X-User-Id": "2", "X-User-Role": "USER" };
const admin = { "X-User-Id": "99", "X-User-Role": "ADMIN" };
const internal = { "X-Internal-Service-Token": "acceptance-internal-token" };
const body = {
  items: [{ productId: 10, quantity: 1 }],
  receiverName: "Acceptance buyer",
  receiverPhone: "13800008000",
  receiverProvince: "Zhejiang",
  receiverCity: "Hangzhou",
  receiverDetailAddress: "Acceptance address",
};

async function createOrder(request: APIRequestContext, key: string) {
  const response = await request.post("/api/order/create", {
    headers: { ...user, "Idempotency-Key": key }, data: body,
  });
  expect(response.status()).toBe(200);
  const payload = await response.json();
  expect(payload.code).toBe(0);
  return payload.data.id as number;
}

test("UC11-UC15 independent order-service acceptance flow", async ({ request }) => {
  const unauthorized = await request.get("/api/order/list", {
    headers: { "X-Request-Id": "acceptance-request", "X-Trace-Id": "acceptance-trace" },
  });
  expect(unauthorized.status()).toBe(401);
  expect(unauthorized.headers()["x-request-id"]).toBe("acceptance-request");
  expect(unauthorized.headers()["x-trace-id"]).toBe("acceptance-trace");

  const runtimeOpenApi = await request.get("/v3/api-docs");
  expect(runtimeOpenApi.status()).toBe(200);
  const runtimeDocument = await runtimeOpenApi.json();
  const methods = new Set(["get", "post", "put", "delete", "patch"]);
  const operationCount = Object.values(runtimeDocument.paths || {}).reduce(
    (count: number, pathItem: any) => count + Object.keys(pathItem).filter((key) => methods.has(key)).length,
    0,
  );
  expect(operationCount).toBe(36);
  expect(runtimeDocument.paths["/internal/orders/secondhand"]?.post).toBeTruthy();
  expect(runtimeDocument.paths["/api/admin/orders/{id}"]?.get).toBeTruthy();

  const fulfilled = await createOrder(request, "uc11-create");
  expect((await request.get(`/api/order/detail/${fulfilled}`, { headers: user })).status()).toBe(200);
  expect((await request.get("/api/order/list", { headers: user })).status()).toBe(200);

  const paid = await request.post(`/api/order/${fulfilled}/pay`, {
    headers: { ...user, "Idempotency-Key": "uc12-pay" }, data: { payMode: "COIN" },
  });
  expect((await paid.json()).data.orderStatusKey).toBe("PENDING_SHIP");
  expect((await request.post(`/api/order/${fulfilled}/ship`, {
    headers: { ...seller, "Idempotency-Key": "uc13-ship" }, data: { deliveryNo: "SF-ACCEPTANCE" },
  })).status()).toBe(200);
  expect((await request.post(`/api/order/${fulfilled}/confirm-receive`, {
    headers: { ...user, "Idempotency-Key": "uc13-receive" },
  })).status()).toBe(200);
  expect((await request.post(`/api/order/${fulfilled}/review/items`, {
    headers: { ...user, "Idempotency-Key": "uc15-review" },
    data: { items: [{ productType: "NEW", productId: 10, score: 5, content: "accepted" }] },
  })).status()).toBe(200);
  expect((await request.get("/api/review/my", { headers: user })).status()).toBe(200);

  const cancelled = await createOrder(request, "uc12-cancel-create");
  expect((await request.post(`/api/order/${cancelled}/cancel`, {
    headers: { ...user, "Idempotency-Key": "uc12-cancel" },
  })).status()).toBe(200);

  const refunded = await createOrder(request, "uc14-create");
  await request.post(`/api/order/${refunded}/pay`, {
    headers: { ...user, "Idempotency-Key": "uc14-pay" }, data: { payMode: "COIN" },
  });
  expect((await request.post(`/api/order/${refunded}/refund`, {
    headers: { ...user, "Idempotency-Key": "uc14-request" }, data: { reason: "acceptance" },
  })).status()).toBe(200);
  expect((await request.post(`/api/admin/orders/${refunded}/refund/approve`, {
    headers: { ...admin, "Idempotency-Key": "uc14-approve" }, data: { remark: "approved" },
  })).status()).toBe(200);

  const info = await request.get("/actuator/info");
  expect(await info.text()).toContain("acceptance");
  expect((await request.get("/actuator/health/liveness")).status()).toBe(200);
  expect((await request.get("/actuator/health/readiness")).status()).toBe(200);
  expect((await request.get("/actuator/flyway")).status()).toBe(404);
});

test("UC20 secondhand order uses the same independently deployed fulfillment state machine", async ({ request }) => {
  const createdResponse = await request.post("/internal/orders/secondhand", {
    headers: internal,
    data: {
      tradeType: "DIRECT",
      tradeId: "acceptance-uc20",
      orderBusinessKey: "SECONDHAND:DIRECT:acceptance-uc20",
      buyerUserId: 1,
      sellerUserId: 2,
      productId: 88,
      productName: "Acceptance secondhand item",
      price: 50,
      receiverName: "Acceptance buyer",
      receiverPhone: "13800008000",
      receiverProvince: "Zhejiang",
      receiverCity: "Hangzhou",
      receiverDetailAddress: "Acceptance address",
    },
  });
  expect(createdResponse.status()).toBe(200);
  const orderId = Number((await createdResponse.json()).data.orderId);

  const replay = await request.post("/internal/orders/secondhand", {
    headers: internal,
    data: {
      tradeType: "DIRECT",
      tradeId: "acceptance-uc20",
      orderBusinessKey: "SECONDHAND:DIRECT:acceptance-uc20",
      buyerUserId: 1,
      sellerUserId: 2,
      productId: 88,
      productName: "Acceptance secondhand item",
      price: 50,
      receiverName: "Acceptance buyer",
      receiverPhone: "13800008000",
      receiverProvince: "Zhejiang",
      receiverCity: "Hangzhou",
      receiverDetailAddress: "Acceptance address",
    },
  });
  expect(replay.status()).toBe(200);
  expect(Number((await replay.json()).data.orderId)).toBe(orderId);

  expect((await request.post(`/api/order/${orderId}/pay`, {
    headers: { ...user, "Idempotency-Key": "uc20-pay" }, data: { payMode: "COIN" },
  })).status()).toBe(200);
  expect((await request.post(`/api/order/${orderId}/ship`, {
    headers: { ...seller, "Idempotency-Key": "uc20-ship" }, data: { deliveryNo: "SF-UC20" },
  })).status()).toBe(200);
  const received = await request.post(`/api/order/${orderId}/confirm-receive`, {
    headers: { ...user, "Idempotency-Key": "uc20-receive" },
  });
  expect((await received.json()).data.orderStatusKey).toBe("RECEIVED");
  const repeated = await request.post(`/api/order/${orderId}/confirm-receive`, {
    headers: { ...user, "Idempotency-Key": "uc20-receive" },
  });
  expect((await repeated.json()).data.id).toBe(orderId);
  const snapshot = await request.get(`/internal/orders/${orderId}/snapshot`, { headers: internal });
  expect((await snapshot.json()).status).toBe("RECEIVED");
});
