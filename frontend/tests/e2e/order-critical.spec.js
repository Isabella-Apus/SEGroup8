import { expect, test } from "@playwright/test";
import { seedUserSession } from "./helpers";

function ok(data) {
  return { status: 200, contentType: "application/json", body: JSON.stringify({ code: 0, message: "success", data }) };
}

test("用户可在订单详情提交退款申请", async ({ page }) => {
  await seedUserSession(page, "USER");
  await page.route("**/api/user/profile", async (route) => route.fulfill(ok({ id: 1, role: "USER", username: "buyer1" })));
  await page.route("**/api/order/detail/101", async (route) =>
    route.fulfill(
      ok({
        id: 101,
        orderNo: "ORD101",
        orderStatus: 2,
        orderStatusName: "待收货",
        refundStatus: 0,
        refundStatusName: "无",
        createTime: "2026-04-08 10:00:00",
        items: [{ id: 1, productType: "NEW", productId: 1001, productName: "测试商品", price: 99, quantity: 1 }]
      })
    )
  );
  await page.route("**/api/order/101/refund", async (route) => route.fulfill(ok({ id: 101, refundStatus: 1, refundStatusName: "退款中" })));

  await page.goto("/order/101");
  await page.getByRole("button", { name: "申请退货" }).click();
  const refundDialog = page.getByRole("dialog", { name: "申请退货/退款" });
  await expect(refundDialog).toBeVisible();
  await refundDialog.locator(".el-select").first().click();
  await page.getByRole("option", { name: "质量问题/损坏" }).click();
  await page.getByRole("button", { name: "提交申请" }).click();
  await expect(page.getByText("已提交退货申请")).toBeVisible();
});

test("管理员可在抽屉同意退款", async ({ page }) => {
  await seedUserSession(page, "ADMIN");
  await page.route("**/api/user/profile", async (route) => route.fulfill(ok({ id: 2, role: "ADMIN", username: "admin1" })));
  await page.route("**/api/admin/orders/list**", async (route) =>
    route.fulfill(
      ok({
        records: [{ id: 101, orderNo: "ORD101", buyerUserId: 1, orderStatusName: "待发货", totalAmount: 99, createTime: "2026-04-08 10:00:00" }],
        total: 1
      })
    )
  );
  await page.route("**/api/admin/orders/detail/101", async (route) =>
    route.fulfill(
      ok({
        id: 101,
        orderNo: "ORD101",
        orderStatus: 1,
        orderStatusName: "待发货",
        refundStatus: 1,
        refundStatusName: "退款中",
        refundReason: "质量问题",
        items: [{ id: 1, productType: "NEW", productName: "测试商品", price: 99, quantity: 1 }]
      })
    )
  );
  await page.route("**/api/admin/orders/101/after-sale-logs", async (route) => route.fulfill(ok([])));
  await page.route("**/api/admin/orders/101/refund/approve", async (route) =>
    route.fulfill(ok({ id: 101, refundStatus: 2, refundStatusName: "已退款", orderStatusName: "已关闭" }))
  );

  await page.goto("/admin/orders");
  await page.getByRole("button", { name: "查看详情" }).first().click();
  await page.getByRole("button", { name: "同意退货" }).click();
  await page.getByRole("button", { name: "同意退货并退款" }).click();
  await expect(page.getByText("已同意退货并完成退款")).toBeVisible();
});

test("重复支付请求时显示可恢复错误建议", async ({ page }) => {
  await seedUserSession(page, "USER");
  await page.route("**/api/user/profile", async (route) => route.fulfill(ok({ id: 1, role: "USER", username: "buyer1" })));
  await page.route("**/api/order/list**", async (route) =>
    route.fulfill(
      ok({
        records: [{ id: 201, orderNo: "ORD201", orderStatus: 0, orderStatusName: "待付款", refundStatus: 0, totalAmount: 88, createTime: "2026-04-08 10:00:00", items: [] }],
        total: 1
      })
    )
  );
  await page.route("**/api/order/201/pay", async (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ code: 409, message: "请求重复，请勿重复提交", data: null })
    })
  );

  await page.goto("/order");
  await page.getByRole("button", { name: "立即付款" }).first().click();
  await page.getByRole("button", { name: "确认付款" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "请求重复，请勿重复提交" }).first()).toBeVisible();
});

