import { test, expect } from "../fixtures";
import { login, logout } from "../helpers/auth";
import { assertNoVisibleError, waitForApiResponse } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC13 new-product fulfillment", () => {
    test("seller ships, buyer views logistics and confirms receipt", async ({ page }, testInfo) => {
        await login(page, { username: process.env.E2E_SELLER_USERNAME || "seller", password: process.env.E2E_SELLER_PASSWORD || "seller123", role: "OFFICIAL_SELLER" });
        let listPromise = waitForApiResponse(page, "/api/order/seller/list");
        await page.goto("/merchant/orders");
        await listPromise;
        const row = page.locator("tr").filter({ hasText: "UC13-E2E-NEW-FULFILLMENT" });
        await expect(row).toBeVisible();
        const shipResponsePromise = waitForApiResponse(page, "/api/order/13001/ship", 200, "POST");
        await row.getByRole("button", { name: "发货" }).click();
        await page.locator(".el-message-box__btns button").last().click();
        expect((await shipResponsePromise).status()).toBe(200);

        await logout(page);
        await login(page, { username: process.env.E2E_USERNAME || "user", password: process.env.E2E_PASSWORD || "user123", role: "USER" });
        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const card = page.locator(".order-card").filter({ hasText: "UC13-E2E-NEW-FULFILLMENT" });
        await expect(card).toBeVisible();
        await expect(card).toContainText("待收货");

        const tracePromise = waitForApiResponse(page, "/api/logistics/order/13001/trace");
        await card.getByRole("button", { name: "查看物流" }).click();
        expect((await tracePromise).status()).toBe(200);
        await expect(page.getByText("物流轨迹").first()).toBeVisible();
        await page.goBack();
        await page.waitForURL(/\/order/);
        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.reload();
        await listPromise;
        const refreshedCard = page.locator(".order-card").filter({ hasText: "UC13-E2E-NEW-FULFILLMENT" });
        await expect(refreshedCard).toBeVisible();

        const receivePromise = waitForApiResponse(page, "/api/order/13001/confirm-receive", 200, "POST");
        await refreshedCard.getByRole("button", { name: "确认收货" }).click();
        await page.locator(".el-message-box__btns button").last().click();
        expect((await receivePromise).status()).toBe(200);

        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.reload();
        await listPromise;
        await expect(page.locator(".order-card").filter({ hasText: "UC13-E2E-NEW-FULFILLMENT" })).toContainText("待评价");
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc13-fulfillment-persisted");
    });
});
