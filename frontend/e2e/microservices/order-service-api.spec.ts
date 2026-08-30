import { expect, test, type APIRequestContext } from "@playwright/test";

const user = { "X-User-Id": "1", "X-User-Role": "USER" };
const seller = { "X-User-Id": "2", "X-User-Role": "USER" };
const admin = { "X-User-Id": "99", "X-User-Role": "ADMIN" };
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
    headers: { ...user, "Idempotency-Key": key }, json: body,
  });
  expect(response.status()).toBe(200);
  const payload = await response.json();
  expect(payload.code).toBe(0);
  return payload.data.id as number;
}

test("UC11-UC15 independent order-service acceptance flow", async ({ request }) => {
  const unauthorized = await request.get("/api/order/list");
  expect(unauthorized.status()).toBe(401);

  const fulfilled = await createOrder(request, "uc11-create");
  expect((await request.get(`/api/order/detail/${fulfilled}`, { headers: user })).status()).toBe(200);
  expect((await request.get("/api/order/list", { headers: user })).status()).toBe(200);

  const paid = await request.post(`/api/order/${fulfilled}/pay`, {
    headers: { ...user, "Idempotency-Key": "uc12-pay" }, json: { payMode: "COIN" },
  });
  expect((await paid.json()).data.orderStatusKey).toBe("PENDING_SHIP");
  expect((await request.post(`/api/order/${fulfilled}/ship`, {
    headers: { ...seller, "Idempotency-Key": "uc13-ship" }, json: { deliveryNo: "SF-ACCEPTANCE" },
  })).status()).toBe(200);
  expect((await request.post(`/api/order/${fulfilled}/confirm-receive`, {
    headers: { ...user, "Idempotency-Key": "uc13-receive" },
  })).status()).toBe(200);
  expect((await request.post(`/api/order/${fulfilled}/review/items`, {
    headers: { ...user, "Idempotency-Key": "uc15-review" },
    json: { items: [{ productType: "NEW", productId: 10, score: 5, content: "accepted" }] },
  })).status()).toBe(200);
  expect((await request.get("/api/review/my", { headers: user })).status()).toBe(200);

  const cancelled = await createOrder(request, "uc12-cancel-create");
  expect((await request.post(`/api/order/${cancelled}/cancel`, {
    headers: { ...user, "Idempotency-Key": "uc12-cancel" },
  })).status()).toBe(200);

  const refunded = await createOrder(request, "uc14-create");
  await request.post(`/api/order/${refunded}/pay`, {
    headers: { ...user, "Idempotency-Key": "uc14-pay" }, json: { payMode: "COIN" },
  });
  expect((await request.post(`/api/order/${refunded}/refund`, {
    headers: { ...user, "Idempotency-Key": "uc14-request" }, json: { reason: "acceptance" },
  })).status()).toBe(200);
  expect((await request.post(`/api/admin/orders/${refunded}/refund/approve`, {
    headers: { ...admin, "Idempotency-Key": "uc14-approve" }, json: { remark: "approved" },
  })).status()).toBe(200);

  const info = await request.get("/actuator/info");
  expect(await info.text()).toContain("acceptance");
  expect((await request.get("/actuator/health/liveness")).status()).toBe(200);
  expect((await request.get("/actuator/health/readiness")).status()).toBe(200);
  expect((await request.get("/actuator/flyway")).status()).toBe(404);
});
