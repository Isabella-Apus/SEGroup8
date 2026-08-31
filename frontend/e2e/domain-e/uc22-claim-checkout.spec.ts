import { test, expect } from "../fixtures";
import { assertNoVisibleError } from "../helpers/http";
import {
    bearer,
    captureDomainEEvidence,
    domainEToken,
    expectBusinessFailure,
    expectBusinessSuccess,
    loginAsDomainE,
    uniqueName,
} from "../helpers/domain-e";

const responseTimeout = 30_000;

function localDateTime(offsetDays: number): string {
    return new Date(Date.now() + offsetDays * 86_400_000)
        .toISOString()
        .slice(0, 19);
}

function activeVoucherBody(name: string) {
    return {
        name,
        type: 1,
        discountAmount: 10,
        minAmount: 100,
        noThreshold: false,
        totalCount: 50,
        grabStartTime: localDateTime(-1),
        grabEndTime: localDateTime(2),
        startTime: localDateTime(-1),
        endTime: localDateTime(7),
    };
}

test.describe("@DOMAIN_E @UC22 voucher claim and checkout", () => {
    test.describe.configure({ timeout: 90_000 });

    test("buyer claims and uses a voucher in a paid order through the real UI", async ({
        page,
        request,
    }, testInfo) => {
        const voucherName = uniqueName("UC22-E2E");
        const sellerToken = await domainEToken(request, "OFFICIAL_SELLER");
        const createdVoucher = await expectBusinessSuccess<{ id: number }>(
            await request.post("/api/voucher/seller", {
                headers: bearer(sellerToken),
                data: activeVoucherBody(voucherName),
            }),
        );
        const voucherId = Number(createdVoucher.id);
        expect(voucherId).toBeGreaterThan(0);

        await loginAsDomainE(page, "BUYER");
        const centerResponse = page.waitForResponse(
            (response) => response.url().includes("/api/voucher/list")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/product/coupons");
        await expectBusinessSuccess(await centerResponse);

        const voucherCard = () => page.locator(".coupon-card").filter({
            hasText: voucherName,
        });
        await expect(voucherCard()).toBeVisible();
        const claimResponse = page.waitForResponse(
            (response) => response.url().endsWith(`/api/voucher/${voucherId}/claim`)
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await voucherCard().getByRole("button", { name: "立即领取", exact: true }).click();
        await expectBusinessSuccess(await claimResponse);
        await expect(page.getByText("领取成功", { exact: true })).toBeVisible();
        await expect(voucherCard()).toContainText("未使用");

        const buyerToken = await domainEToken(request, "BUYER");
        const addresses = await expectBusinessSuccess<Array<{
            detailAddress: string;
        }>>(await request.get("/api/user/addresses", {
            headers: bearer(buyerToken),
        }));
        if (!addresses.some((item) => item.detailAddress === "UC22 E2E Address")) {
            await expectBusinessSuccess(await request.post("/api/user/addresses", {
                headers: bearer(buyerToken),
                data: {
                    receiverName: "UC22 Buyer",
                    receiverPhone: "13800138022",
                    province: "Beijing",
                    city: "Beijing",
                    detailAddress: "UC22 E2E Address",
                    isDefault: 1,
                },
            }));
        }
        const knownAddressDetails = new Set([
            ...addresses.map((item) => item.detailAddress),
            "UC22 E2E Address",
        ]);

        const duplicateClaim = await request.post(`/api/voucher/${voucherId}/claim`, {
            headers: bearer(buyerToken),
        });
        const duplicatePayload = await expectBusinessFailure(duplicateClaim);
        expect(String(duplicatePayload.message)).toContain("领取");

        const unavailableReasons = await expectBusinessSuccess<string[]>(
            await request.get("/api/voucher/my/available/reasons", {
                headers: bearer(buyerToken),
                params: { shopIds: "1", totalAmount: "50.00" },
            }),
        );
        expect(unavailableReasons.join(" ")).toContain("门槛不足");

        const productResponse = page.waitForResponse(
            (response) => response.url().includes("/api/product/detail/1")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/product/1");
        await expectBusinessSuccess(await productResponse);
        await expect(page.getByRole("heading", { name: "Container Demo Keyboard" }))
            .toBeVisible();

        const couponSelect = page.locator(".coupon-select-row .el-select");
        await expect(couponSelect).not.toHaveClass(/is-loading/);
        await couponSelect.click();
        await page.getByRole("option", { name: new RegExp(voucherName) }).click();
        await expect(page.locator(".order-preview")).toContainText("已优惠 ¥10.00");
        await expect(page.locator(".order-preview")).toContainText("¥289.00");

        await page.getByRole("button", { name: "立即购买", exact: true }).click();
        const addressDialog = page.getByRole("dialog", { name: "收货地址确认" });
        await expect(addressDialog).toBeVisible();
        const addressDialogText = await addressDialog.innerText();
        expect(
            [...knownAddressDetails].some((detail) => addressDialogText.includes(detail)),
            "checkout must show one of the buyer's persisted addresses",
        ).toBe(true);

        const createResponsePromise = page.waitForResponse(
            (response) => response.url().endsWith("/api/order/create")
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await addressDialog.getByRole("button", { name: "提交订单", exact: true }).click();
        const order = await expectBusinessSuccess<{
            id: number;
            totalAmount: number;
            voucherDiscountAmount: number;
            payableAmount: number;
        }>(await createResponsePromise);
        const orderId = Number(order.id);
        expect(orderId).toBeGreaterThan(0);
        expect(Number(order.totalAmount)).toBe(299);
        expect(Number(order.voucherDiscountAmount)).toBe(10);
        expect(Number(order.payableAmount)).toBe(289);

        await expect(page).toHaveURL(new RegExp(`/order/${orderId}`));
        const payDialog = page.locator(".order-pay-dialog");
        await expect(payDialog).toBeVisible();
        await expect(payDialog).toContainText("¥289.00");
        const payResponsePromise = page.waitForResponse(
            (response) => response.url().endsWith(`/api/order/${orderId}/pay`)
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await payDialog.getByRole("button", { name: "我已支付", exact: true }).click();
        const paidOrder = await expectBusinessSuccess<{ orderStatus: number }>(
            await payResponsePromise,
        );
        expect(Number(paidOrder.orderStatus)).toBe(1);
        await expect(page.getByText("待发货", { exact: true }).first()).toBeVisible();
        await expect(page.getByText("-￥10.00", { exact: true })).toBeVisible();
        await expect(page.getByText("￥289.00", { exact: true })).toBeVisible();

        const myCouponsResponse = page.waitForResponse(
            (response) => response.url().includes("/api/voucher/my")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/product/coupons");
        await expectBusinessSuccess(await myCouponsResponse);
        await page.getByRole("tab", { name: /已使用/ }).click();
        await expect(voucherCard()).toContainText("已使用");

        const refreshedCouponsResponse = page.waitForResponse(
            (response) => response.url().includes("/api/voucher/my")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.reload();
        await expectBusinessSuccess(await refreshedCouponsResponse);
        await page.getByRole("tab", { name: /已使用/ }).click();
        await expect(voucherCard()).toContainText("已使用");

        const mine = await expectBusinessSuccess<{ records: Array<{
            id: number;
            myStatus: number;
        }> }>(await request.get("/api/voucher/my", {
            headers: bearer(buyerToken),
            params: { page: 1, pageSize: 100, mallType: "NEW" },
        }));
        const persistedVoucher = mine.records.find((item) => Number(item.id) === voucherId);
        expect(persistedVoucher?.myStatus).toBe(2);

        const persistedOrder = await expectBusinessSuccess<{
            id: number;
            voucherId: number;
            voucherDiscountAmount: number;
            payableAmount: number;
            orderStatus: number;
        }>(await request.get(`/api/order/detail/${orderId}`, {
            headers: bearer(buyerToken),
        }));
        expect(Number(persistedOrder.voucherId)).toBe(voucherId);
        expect(Number(persistedOrder.voucherDiscountAmount)).toBe(10);
        expect(Number(persistedOrder.payableAmount)).toBe(289);
        expect(Number(persistedOrder.orderStatus)).toBe(1);

        await assertNoVisibleError(page);
        await captureDomainEEvidence(
            page,
            testInfo,
            "UC22",
            "uc22-voucher-used-after-paid-order-refresh",
        );
    });
});
