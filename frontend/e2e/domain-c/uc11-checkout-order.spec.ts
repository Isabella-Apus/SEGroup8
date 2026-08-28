import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import { assertNoVisibleError } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC11 checkout and order creation", () => {
    test("persists cart checkout and renders the same order after refresh", async ({
        page,
        testAccount,
    }, testInfo) => {
        await login(page, testAccount);
        await page.evaluate(() => {
            window.localStorage.removeItem("segroup8_cart_items");
            window.localStorage.removeItem("segroup8_secondhand_cart_items");
        });

        const productsPromise = page.waitForResponse((response) =>
            response.url().includes("/api/product/list")
            && response.request().method() === "GET"
            && response.status() === 200,
        );
        await page.goto("/product");
        expect((await productsPromise).ok()).toBeTruthy();

        const productCard = page.locator(".product-card").filter({
            hasText: "Container Demo Keyboard",
        });
        await expect(productCard).toBeVisible();
        await productCard.getByRole("button", { name: "加购" }).click();
        await expect(page.getByText("已加入购物车", { exact: true })).toBeVisible();

        const detailPromise = page.waitForResponse((response) =>
            response.url().includes("/api/product/detail/1")
            && response.request().method() === "GET"
            && response.status() === 200,
        );
        await page.goto("/cart");
        expect((await detailPromise).ok()).toBeTruthy();
        await expect(page.getByRole("heading", { name: "购物车" })).toBeVisible();
        await expect(page.getByText("Container Demo Keyboard", { exact: true })).toBeVisible();

        await page.getByText("全选", { exact: true }).click();
        await expect(page.getByText("1 / 1 件商品", { exact: true })).toBeVisible();
        await page.getByRole("button", { name: "按分组结算" }).click();

        const addressDialog = page.getByRole("dialog", { name: "确认收货地址" });
        await expect(addressDialog).toBeVisible();
        await expect(addressDialog).toContainText("Container E2E Address");

        const createResponsePromise = page.waitForResponse((response) =>
            response.url().includes("/api/order/create")
            && response.request().method() === "POST",
        );
        await addressDialog.getByRole("button", { name: "确认下单" }).click();
        const createResponse = await createResponsePromise;
        expect(createResponse.status()).toBe(200);
        const createdPayload = await createResponse.json();
        expect(createdPayload?.code).toBe(0);
        expect(createdPayload?.data?.orderStatus).toBe(0);
        expect(createdPayload?.data?.items).toHaveLength(1);
        const orderId = Number(createdPayload?.data?.id);
        expect(orderId).toBeGreaterThan(0);

        await expect(page).toHaveURL(new RegExp(`/order/${orderId}`));
        await expect(page.getByRole("heading", { name: "订单详情" })).toBeVisible();
        await expect(page.getByText("Container Demo Keyboard", { exact: true })).toBeVisible();
        await expect(page.getByText("待付款", { exact: true }).first()).toBeVisible();
        await assertNoVisibleError(page);

        const persistedResponsePromise = page.waitForResponse((response) =>
            response.url().includes(`/api/order/detail/${orderId}`)
            && response.request().method() === "GET"
            && response.status() === 200,
        );
        await page.reload();
        const persistedResponse = await persistedResponsePromise;
        const persistedPayload = await persistedResponse.json();
        expect(persistedPayload?.code).toBe(0);
        expect(Number(persistedPayload?.data?.id)).toBe(orderId);
        expect(persistedPayload?.data?.orderStatus).toBe(0);
        expect(persistedPayload?.data?.items?.[0]?.productName)
            .toBe("Container Demo Keyboard");

        await expect(page.getByRole("heading", { name: "订单详情" })).toBeVisible();
        await expect(page.getByText("Container Demo Keyboard", { exact: true })).toBeVisible();
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc11-checkout-order-persisted");
    });
});
