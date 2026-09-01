import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import { assertNoVisibleError, waitForApiResponse } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C order fulfillment shared infrastructure", () => {
    test("logs in and renders the persisted order view", async ({
        page,
        testAccount,
    }, testInfo) => {
        await login(page, testAccount);

        const token = await page.evaluate(() =>
            window.localStorage.getItem("segroup8_token"),
        );
        expect(token).toBeTruthy();
        const contractResponse = await page.context().request.get(
            "/api/order/list",
            { headers: { Authorization: `Bearer ${token}` } },
        );
        expect(contractResponse.status()).toBe(200);
        expect((await contractResponse.json())?.code).toBe(0);

        const orderResponsePromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        const orderResponse = await orderResponsePromise;
        expect(orderResponse.status()).toBe(200);
        await expect(page).toHaveURL(/\/order$/);
        await expect(
            page.getByRole("heading", { name: "我的订单" }),
        ).toBeVisible();
        await expect(page.locator(".skeleton-panel")).toHaveCount(0);
        await expect(page.locator(".el-loading-mask")).toHaveCount(0);
        await expect(
            page.getByText("全部订单", { exact: true }).first(),
        ).toBeVisible();
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "domain-c-platform-health");
    });
});
