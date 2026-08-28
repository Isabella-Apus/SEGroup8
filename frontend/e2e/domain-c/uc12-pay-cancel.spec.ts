import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import { assertNoVisibleError, waitForApiResponse } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC12 payment and cancellation", () => {
    test("pays and cancels persisted orders through the browser", async ({
        page,
        testAccount,
    }, testInfo) => {
        await login(page, testAccount);
        const initialListPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await initialListPromise;

        const payCard = page.locator(".order-card").filter({ hasText: "UC12-E2E-PAY" });
        await expect(payCard).toBeVisible();
        const payResponsePromise = waitForApiResponse(page, "/api/order/12001/pay", 200, "POST");
        await payCard.getByRole("button", { name: "立即付款" }).click();
        const payDialog = page.locator(".order-pay-dialog");
        await expect(payDialog).toBeVisible();
        await payDialog.getByRole("button", { name: "我已支付" }).click();
        const payResponse = await payResponsePromise;
        expect((await payResponse.json())?.code).toBe(0);

        let listPromise = waitForApiResponse(page, "/api/order/list");
        await page.reload();
        await listPromise;
        await expect(page.locator(".order-card").filter({ hasText: "UC12-E2E-PAY" }))
            .toContainText("待发货");

        const cancelCard = page.locator(".order-card").filter({ hasText: "UC12-E2E-CANCEL" });
        await expect(cancelCard).toBeVisible();
        const cancelResponsePromise = waitForApiResponse(page, "/api/order/12002/cancel", 200, "POST");
        await cancelCard.getByRole("button", { name: "取消订单" }).click();
        await page.getByRole("button", { name: "确认取消" }).click();
        const cancelResponse = await cancelResponsePromise;
        expect((await cancelResponse.json())?.code).toBe(0);

        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.reload();
        await listPromise;
        await expect(page.locator(".order-card").filter({ hasText: "UC12-E2E-CANCEL" }))
            .toContainText("已关闭");
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc12-pay-cancel-persisted");
    });
});
