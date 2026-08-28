import { expect, test } from "../fixtures";
import {
    apiToken,
    bearer,
    captureEvidence,
    expectBusinessFailure,
    expectBusinessSuccess,
    loginAsDomainD,
    uniqueName,
} from "./support";

const responseTimeout = 30_000;
const productPrice = 86;
const productImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='600'%3E%3Crect width='600' height='600' fill='%23dff8f1'/%3E%3C/svg%3E";

test.describe("@DOMAIN_D @UC20 secondhand fulfillment", () => {
    test.describe.configure({ timeout: 150_000 });

    test("seller ships, buyer sees logistics and confirms receipt with one settlement", async ({
        browser,
        page,
        request,
    }, testInfo) => {
        const sellerToken = await apiToken(request, "seller");
        const buyerToken = await apiToken(request, "buyer");
        const sellerBefore = await expectBusinessSuccess<any>(
            await request.get("/api/finance/dashboard", { headers: bearer(sellerToken) }),
        );
        const sellerBalanceBefore = Number(sellerBefore.personalBalance);
        const productName = uniqueName("UC20-E2E");

        const created = await expectBusinessSuccess<any>(
            await request.post("/api/secondhand/seller", {
                headers: bearer(sellerToken),
                data: {
                    name: productName,
                    cover: productImage,
                    images: [productImage],
                    description: "UC20 real Compose fulfillment fixture",
                    originPrice: 168,
                    salePrice: productPrice,
                    categoryId: 1,
                    subCategoryId: 101,
                    conditionLevel: "95%",
                    isNegotiable: 0,
                    status: 1,
                },
            }),
        );
        const productId = Number(created.id);
        expect(productId).toBeGreaterThan(0);

        const ordered = await expectBusinessSuccess<any>(
            await request.post(`/api/secondhand/${productId}/buy`, {
                headers: bearer(buyerToken),
                data: { addressId: 1 },
            }),
        );
        const orderId = Number(ordered.id);
        expect(orderId).toBeGreaterThan(0);
        expect(Number(ordered.orderStatus)).toBe(0);
        expect(Number(ordered.payStatus)).toBe(0);

        const paid = await expectBusinessSuccess<any>(
            await request.post(`/api/order/${orderId}/pay`, {
                headers: bearer(buyerToken),
                data: { payMode: "THIRD_PARTY", payChannel: "WECHAT" },
            }),
        );
        expect(Number(paid.orderStatus)).toBe(1);
        expect(Number(paid.payStatus)).toBe(1);

        await loginAsDomainD(page, "seller");
        await page.goto("/secondhand/sold");
        const soldCard = page.locator(".sold-card").filter({ hasText: productName });
        await expect(soldCard).toBeVisible();
        await expect(soldCard).toContainText("待我发货");
        await soldCard.getByRole("button", { name: "确认发货", exact: true }).click();

        const shipDialog = page.getByRole("dialog", { name: "确认发货" });
        const provinceField = shipDialog.locator(".el-form-item").filter({ hasText: "发货省份" });
        await provinceField.locator(".el-select__wrapper").click();
        await page.getByRole("option", { name: "广东省", exact: true }).click();
        await shipDialog.getByRole("textbox", { name: "发货城市" }).fill("广州市");
        await shipDialog.getByRole("textbox", { name: "详细地址" }).fill("天河区 UC20 发货点");
        const shipResponse = page.waitForResponse(
            (response) => response.url().endsWith(`/api/order/${orderId}/ship`)
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await shipDialog.getByRole("button", { name: "确认发货", exact: true }).click();
        const shipped = await expectBusinessSuccess<any>(await shipResponse);
        expect(Number(shipped.orderStatus)).toBe(2);
        expect(String(shipped.logisticsStatus)).toBe("IN_TRANSIT");
        await expect(soldCard).toContainText("已发货");
        await captureEvidence(page, testInfo, "uc20-seller-shipped-order");

        const baseURL = String(testInfo.project.use.baseURL || "http://127.0.0.1:8088");
        const buyerContext = await browser.newContext({ baseURL });
        const buyerPage = await buyerContext.newPage();
        try {
            await loginAsDomainD(buyerPage, "buyer");
            await buyerPage.goto(`/secondhand/orders/${orderId}`);
            await expect(buyerPage.getByText(productName, { exact: true })).toBeVisible();
            await expect(buyerPage.getByText("待收货", { exact: true }).first()).toBeVisible();
            await expect(buyerPage.getByText("广东省分拨中心", { exact: true })).toBeVisible();
            await expect(buyerPage.getByText("包裹已揽收", { exact: true })).toBeVisible();
            await captureEvidence(buyerPage, testInfo, "uc20-buyer-logistics-visible");

            await buyerPage.getByRole("button", { name: "确认收货", exact: true }).click();
            const confirmBox = buyerPage.locator(".el-message-box").filter({ hasText: "确认已收到货物" });
            await expect(confirmBox).toBeVisible();
            const receiveResponse = buyerPage.waitForResponse(
                (response) => response.url().endsWith(`/api/order/${orderId}/confirm-receive`)
                    && response.request().method() === "POST",
                { timeout: responseTimeout },
            );
            await confirmBox.getByRole("button", { name: "确认收货", exact: true }).click();
            const received = await expectBusinessSuccess<any>(await receiveResponse);
            expect(Number(received.orderStatus)).toBe(3);
            expect(Number(received.payStatus)).toBe(1);
            await confirmBox.waitFor({ state: "hidden", timeout: 10_000 });
            await expect(buyerPage.getByText("待评价", { exact: true }).first()).toBeVisible();
            await expect(buyerPage.getByRole("button", { name: "去评价", exact: true })).toBeVisible();
            await captureEvidence(buyerPage, testInfo, "uc20-buyer-pending-review");

            const persisted = await expectBusinessSuccess<any>(
                await request.get(`/api/order/detail/${orderId}`, {
                    headers: bearer(buyerToken),
                }),
            );
            expect(Number(persisted.orderStatus)).toBe(3);
            expect(Number(persisted.payStatus)).toBe(1);
            expect(persisted.receivedTime).toBeTruthy();

            const sellerAfter = await expectBusinessSuccess<any>(
                await request.get("/api/finance/dashboard", { headers: bearer(sellerToken) }),
            );
            expect(Number(sellerAfter.personalBalance)).toBeCloseTo(
                sellerBalanceBefore + productPrice,
                2,
            );

            const repeated = await expectBusinessFailure(
                await request.post(`/api/order/${orderId}/confirm-receive`, {
                    headers: bearer(buyerToken),
                }),
            );
            expect(Number(repeated.code)).toBe(400);
            const sellerAfterRepeated = await expectBusinessSuccess<any>(
                await request.get("/api/finance/dashboard", { headers: bearer(sellerToken) }),
            );
            expect(Number(sellerAfterRepeated.personalBalance)).toBeCloseTo(
                sellerBalanceBefore + productPrice,
                2,
            );
        } finally {
            await buyerContext.close();
        }

        await page.reload();
        const settledCard = page.locator(".sold-card").filter({ hasText: productName });
        await expect(settledCard).toContainText("待买家评价");
        await captureEvidence(page, testInfo, "uc20-seller-settled-once");
    });
});
