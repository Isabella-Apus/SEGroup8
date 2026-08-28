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

test.describe("@DOMAIN_D @UC18 secondhand bargain", () => {
    test.describe.configure({ timeout: 120_000 });

    test("buyer applies, seller confirms in chat, and buyer receives a pending-payment order", async ({
        browser,
        page,
        request,
    }, testInfo) => {
        const sellerToken = await apiToken(request, accounts.seller);
        const buyerToken = await apiToken(request, accounts.user);
        const productName = uniqueName("UC18-E2E");
        const created = await expectBusinessSuccess(
            await request.post("/api/secondhand/seller", {
                headers: bearer(sellerToken),
                data: {
                    name: productName,
                    description: "UC18 real Compose bargain fixture",
                    images: ["/uploads/e2e-secondhand-product.svg"],
                    originPrice: 188,
                    salePrice: 98,
                    categoryId: 1,
                    subCategoryId: 101,
                    conditionLevel: "95%",
                    isNegotiable: 1,
                    status: 1,
                },
            }),
        );
        const productId = Number(created.id);
        expect(productId).toBeGreaterThan(0);

        await login(page, accounts.user);
        await page.goto(`/secondhand/${productId}`);
        await expect(page.getByText(productName, { exact: true })).toBeVisible();
        await page.getByRole("button", { name: "我要议价", exact: true }).click();
        const bargainDialog = page.getByRole("dialog", { name: "发送议价" });
        await bargainDialog.getByRole("spinbutton").fill("76");
        const applyResponse = page.waitForResponse(
            (response) => response.url().endsWith("/api/secondhand/trade/bargain/apply")
                && response.request().method() === "POST",
        );
        await bargainDialog.getByRole("button", { name: "发送给卖家", exact: true }).click();
        const applied = await expectBusinessSuccess(await applyResponse);
        const negotiationId = Number(applied.id);
        expect(negotiationId).toBeGreaterThan(0);
        expect(applied.status).toBe("APPLIED");
        await expect(page).toHaveURL(/\/messages/);
        await expect(page.getByText(productName, { exact: true }).first()).toBeVisible();
        await expect(page.getByText("等待卖家处理").first()).toBeVisible();
        await capture(page, testInfo, "uc18-buyer-application-pending");

        const sellerContext = await browser.newContext({
            baseURL: String(testInfo.project.use.baseURL || "http://127.0.0.1:8088"),
        });
        const sellerPage = await sellerContext.newPage();
        try {
            await login(sellerPage, accounts.seller);
            await sellerPage.goto("/merchant/messages");
            const conversation = sellerPage.locator(".conversation-item").filter({ hasText: productName });
            await expect(conversation).toBeVisible();
            await conversation.click();
            await expect(sellerPage.getByText(productName, { exact: true }).first()).toBeVisible();
            await expect(sellerPage.getByText("¥76.00", { exact: true })).toBeVisible();

            const confirmResponse = sellerPage.waitForResponse(
                (response) => response.url().endsWith("/api/secondhand/trade/bargain/confirm")
                    && response.request().method() === "POST",
            );
            await sellerPage.getByRole("button", { name: "同意并生成订单", exact: true }).click();
            const confirmed = await expectBusinessSuccess(await confirmResponse);
            const orderId = Number(confirmed.orderId);
            expect(confirmed.status).toBe("USED");
            expect(orderId).toBeGreaterThan(0);
            await expect(sellerPage.getByText("已生成订单").first()).toBeVisible();
            await capture(sellerPage, testInfo, "uc18-seller-confirmed-order");

            await page.reload();
            const buyerConversation = page.locator(".conversation-item").filter({ hasText: productName });
            await expect(buyerConversation).toBeVisible();
            await buyerConversation.click();
            await expect(page.getByText("卖家已同意议价").first()).toBeVisible();
            await expect(page.getByRole("button", { name: "去支付订单", exact: true })).toBeVisible();

            const order = await expectBusinessSuccess(
                await request.get(`/api/order/detail/${orderId}`, {
                    headers: bearer(buyerToken),
                }),
            );
            expect(Number(order.orderStatus)).toBe(0);
            expect(Number(order.payStatus)).toBe(0);
            expect(Number(order.totalAmount)).toBe(76);
            expect(order.items).toEqual(expect.arrayContaining([
                expect.objectContaining({
                    productType: "SECONDHAND",
                    productId,
                    productName,
                    price: 76,
                }),
            ]));
            await capture(page, testInfo, "uc18-buyer-confirmed-pending-payment");
        } finally {
            await sellerContext.close();
        }
    });
});

async function capture(
    page: import("@playwright/test").Page,
    testInfo: import("@playwright/test").TestInfo,
    name: string,
) {
    await page.locator(".el-loading-mask").waitFor({ state: "hidden", timeout: 10_000 }).catch(() => {});
    await page.waitForTimeout(300);
    const target = path.join(
        repositoryRoot,
        "04_tests/UC18/evidence/screenshots",
        `${name}.png`,
    );
    mkdirSync(path.dirname(target), { recursive: true });
    await page.screenshot({ path: target, fullPage: true, animations: "disabled" });
    await testInfo.attach(name, { path: target, contentType: "image/png" });
}
