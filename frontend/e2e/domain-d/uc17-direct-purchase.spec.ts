import { mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { expect, test } from "../fixtures";
import {
    accounts,
    apiToken,
    bearer,
    expectBusinessSuccess,
    login,
    uniqueName,
} from "../domain-b/support";

const repositoryRoot = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "../../..",
);

test.describe("@DOMAIN_D @UC17 direct secondhand purchase", () => {
    test.describe.configure({ timeout: 90_000 });

    test("creates one pending order through the real UI and rejects a repeated purchase", async ({
        page,
        request,
    }, testInfo) => {
        const sellerToken = await apiToken(request, accounts.seller);
        const buyerToken = await apiToken(request, accounts.user);
        const productName = uniqueName("UC17-E2E");
        const created = await expectBusinessSuccess(
            await request.post("/api/secondhand/seller", {
                headers: bearer(sellerToken),
                data: {
                    name: productName,
                    description: "UC17 real Compose direct-purchase fixture",
                    images: ["/uploads/e2e-secondhand-product.svg"],
                    originPrice: 199,
                    salePrice: 88,
                    categoryId: 1,
                    subCategoryId: 101,
                    conditionLevel: "90%",
                    isNegotiable: 0,
                    status: 1,
                },
            }),
        );
        const productId = Number(created.id);
        expect(productId).toBeGreaterThan(0);

        await login(page, accounts.user);
        await page.goto(`/secondhand/${productId}`);
        await expect(page.getByText(productName, { exact: true })).toBeVisible();

        await page.getByRole("button", { name: "立即购买", exact: true }).click();
        const addressDialog = page.getByRole("dialog", { name: "确认收货地址" });
        await expect(addressDialog).toContainText("Demo User");

        const buyResponsePromise = page.waitForResponse(
            (response) => response.url().endsWith(`/api/secondhand/${productId}/buy`)
                && response.request().method() === "POST",
        );
        await addressDialog.getByRole("button", { name: "确认下单", exact: true }).click();
        const order = await expectBusinessSuccess(await buyResponsePromise);
        const orderId = Number(order.id);
        expect(orderId).toBeGreaterThan(0);
        expect(Number(order.orderStatus)).toBe(0);
        expect(Number(order.payStatus)).toBe(0);

        const payDialog = page.getByRole("dialog", { name: "确认支付" });
        await expect(payDialog).toContainText("88.00");
        await payDialog.getByRole("button", { name: "稍后支付", exact: true }).click();

        await page.reload();
        await expect(page.getByRole("button", { name: "立即购买", exact: true })).toBeDisabled();
        await expect(page.getByText(/已售/).first()).toBeVisible();

        const persistedOrder = await expectBusinessSuccess(
            await request.get(`/api/order/detail/${orderId}`, {
                headers: bearer(buyerToken),
            }),
        );
        expect(Number(persistedOrder.orderStatus)).toBe(0);
        expect(Number(persistedOrder.payStatus)).toBe(0);
        expect(persistedOrder.items).toEqual(expect.arrayContaining([
            expect.objectContaining({
                productType: "SECONDHAND",
                productId,
                productName,
            }),
        ]));

        const repeated = await request.post(`/api/secondhand/${productId}/buy`, {
            headers: bearer(buyerToken),
            data: { addressId: 1 },
        });
        const repeatedPayload = await repeated.json();
        expect(Number(repeatedPayload.code)).not.toBe(0);

        await page.goto("/secondhand/orders");
        await expect(page.getByText(productName, { exact: true })).toBeVisible();
        await capture(page, testInfo, "uc17-pending-order-and-sold-product");
    });
});

async function capture(
    page: import("@playwright/test").Page,
    testInfo: import("@playwright/test").TestInfo,
    name: string,
) {
    const target = path.join(
        repositoryRoot,
        "04_tests/UC17/evidence/screenshots",
        `${name}.png`,
    );
    mkdirSync(path.dirname(target), { recursive: true });
    await page.screenshot({ path: target, fullPage: true, animations: "disabled" });
    await testInfo.attach(name, { path: target, contentType: "image/png" });
}
