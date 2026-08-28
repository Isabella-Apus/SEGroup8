import { test, expect } from "../fixtures";
import type { APIRequestContext, Response } from "@playwright/test";
import { assertNoVisibleError } from "../helpers/http";
import {
    bearer,
    captureDomainEEvidence,
    domainEToken,
    expectBusinessFailure,
    expectBusinessSuccess,
    loginAsDomainE,
} from "../helpers/domain-e";

const responseTimeout = 30_000;
const rechargeAmount = 23.23;

type Dashboard = {
    personalBalance: number;
    businessBalance: number;
};

type FinanceRecord = {
    orderId: number | null;
    accountType: string;
    tradeType: string;
    amount: number;
};

test.describe("@DOMAIN_E @UC23 wallet and settlement", () => {
    test.describe.configure({ timeout: 120_000 });

    test("buyer recharges and seller receives one persisted business settlement", async ({
        page,
        request,
    }, testInfo) => {
        const buyerToken = await domainEToken(request, "BUYER");
        const sellerToken = await domainEToken(request, "OFFICIAL_SELLER");
        const buyerBefore = await financeDashboard(request, buyerToken);
        const sellerBefore = await financeDashboard(request, sellerToken);
        const buyerRecordsBefore = await walletRecords(request, buyerToken);

        await loginAsDomainE(page, "BUYER");
        const initialDashboardResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/dashboard", "GET"),
            { timeout: responseTimeout },
        );
        const initialRecordsResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/my-wallet/records", "GET"),
            { timeout: responseTimeout },
        );
        await page.goto("/profile");
        await expectBusinessSuccess(await initialDashboardResponse);
        await expectBusinessSuccess(await initialRecordsResponse);
        await expect(page.getByRole("heading", { name: "我的钱包" })).toBeVisible();
        await expect(page.locator(".wallet-balance")).toContainText(
            Number(buyerBefore.personalBalance).toFixed(2),
        );

        await page.getByRole("button", { name: "充值商城币", exact: true }).click();
        const rechargeDialog = page.getByRole("dialog", { name: "充值商城币（模拟）" });
        await rechargeDialog.getByRole("spinbutton").fill(String(rechargeAmount));
        await rechargeDialog.getByRole("radio", { name: "支付宝" }).check({ force: true });
        const rechargeResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/recharge", "POST"),
            { timeout: responseTimeout },
        );
        await rechargeDialog.getByRole("button", { name: "我已支付，确认入账" }).click();
        const recharged = await expectBusinessSuccess<Dashboard>(await rechargeResponse);
        expect(Number(recharged.personalBalance)).toBeCloseTo(
            Number(buyerBefore.personalBalance) + rechargeAmount,
            2,
        );
        expect(Number(recharged.businessBalance)).toBe(Number(buyerBefore.businessBalance));

        const rechargeRow = page.locator(".wallet-panel .el-table__row")
            .filter({ hasText: rechargeAmount.toFixed(2) })
            .first();
        await expect(rechargeRow).toContainText("钱包充值");
        await expect(page.locator(".wallet-balance")).toContainText(
            Number(recharged.personalBalance).toFixed(2),
        );

        const refreshedDashboardResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/dashboard", "GET"),
            { timeout: responseTimeout },
        );
        const refreshedRecordsResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/my-wallet/records", "GET"),
            { timeout: responseTimeout },
        );
        await page.reload();
        await expectBusinessSuccess(await refreshedDashboardResponse);
        await expectBusinessSuccess(await refreshedRecordsResponse);
        await expect(page.locator(".wallet-balance")).toContainText(
            Number(recharged.personalBalance).toFixed(2),
        );
        await expect(rechargeRow).toContainText("钱包充值");

        const buyerRecordsAfter = await walletRecords(request, buyerToken);
        expect(buyerRecordsAfter.length).toBe(buyerRecordsBefore.length + 1);
        expect(buyerRecordsAfter[0]).toMatchObject({
            accountType: "PERSONAL",
            tradeType: "RECHARGE",
        });
        expect(Number(buyerRecordsAfter[0].amount)).toBeCloseTo(rechargeAmount, 2);

        const forbiddenBusinessRecords = await request.get("/api/finance/business/records", {
            headers: bearer(buyerToken),
        });
        const forbiddenPayload = await expectBusinessFailure(forbiddenBusinessRecords);
        expect(Number(forbiddenPayload.code)).toBe(403);

        const addresses = await expectBusinessSuccess<Array<{ detailAddress: string }>>(
            await request.get("/api/user/addresses", { headers: bearer(buyerToken) }),
        );
        if (!addresses.some((item) => item.detailAddress === "UC23 E2E Address")) {
            await expectBusinessSuccess(await request.post("/api/user/addresses", {
                headers: bearer(buyerToken),
                data: {
                    receiverName: "UC23 Buyer",
                    receiverPhone: "13800138023",
                    province: "Beijing",
                    city: "Beijing",
                    detailAddress: "UC23 E2E Address",
                    isDefault: 1,
                },
            }));
        }

        const order = await expectBusinessSuccess<{
            id: number;
            payableAmount: number;
        }>(await request.post("/api/order/create", {
            headers: bearer(buyerToken),
            data: { items: [{ productId: 1, quantity: 1 }] },
        }));
        const orderId = Number(order.id);
        const settlementAmount = Number(order.payableAmount);
        expect(orderId).toBeGreaterThan(0);
        expect(settlementAmount).toBeGreaterThan(0);

        await expectBusinessSuccess(await request.post(`/api/order/${orderId}/pay`, {
            headers: bearer(buyerToken),
            data: { payMode: "THIRD_PARTY", payChannel: "WECHAT" },
        }));
        await expectBusinessSuccess(await request.post(`/api/order/${orderId}/ship`, {
            headers: bearer(sellerToken),
            data: {
                originProvince: "Beijing",
                originCity: "Beijing",
                originDetail: "UC23 E2E Warehouse",
            },
        }));
        await expectBusinessSuccess(await request.post(`/api/order/${orderId}/confirm-receive`, {
            headers: bearer(buyerToken),
        }));

        const duplicateSettlement = await request.post(`/api/order/${orderId}/confirm-receive`, {
            headers: bearer(buyerToken),
        });
        const duplicatePayload = await expectBusinessFailure(duplicateSettlement);
        expect(Number(duplicatePayload.code)).toBe(400);

        const sellerAfter = await financeDashboard(request, sellerToken);
        expect(Number(sellerAfter.personalBalance)).toBe(Number(sellerBefore.personalBalance));
        expect(Number(sellerAfter.businessBalance)).toBeCloseTo(
            Number(sellerBefore.businessBalance) + settlementAmount,
            2,
        );

        await loginAsDomainE(page, "OFFICIAL_SELLER");
        const sellerDashboardResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/dashboard", "GET"),
            { timeout: responseTimeout },
        );
        const sellerRecordsResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/business/records", "GET"),
            { timeout: responseTimeout },
        );
        await page.goto("/merchant/finance");
        await expectBusinessSuccess(await sellerDashboardResponse);
        await expectBusinessSuccess(await sellerRecordsResponse);
        await expect(page.locator("h2.page-title", { hasText: "财务看板" })).toBeVisible();
        await expect(page.locator(".balance-card.business")).toContainText(
            Number(sellerAfter.businessBalance).toFixed(2),
        );

        const settlementRow = page.locator(".el-table__row")
            .filter({ hasText: String(orderId) })
            .first();
        await expect(settlementRow).toContainText("经营账户入账");
        await expect(settlementRow).toContainText(settlementAmount.toFixed(2));

        const persistedDashboardResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/dashboard", "GET"),
            { timeout: responseTimeout },
        );
        const persistedRecordsResponse = page.waitForResponse(
            isFinanceResponse("/api/finance/business/records", "GET"),
            { timeout: responseTimeout },
        );
        await page.reload();
        await expectBusinessSuccess(await persistedDashboardResponse);
        await expectBusinessSuccess(await persistedRecordsResponse);
        await expect(page.locator(".balance-card.business")).toContainText(
            Number(sellerAfter.businessBalance).toFixed(2),
        );
        await expect(settlementRow).toContainText("经营账户入账");

        const sellerRecords = await businessRecords(request, sellerToken);
        const persistedSettlement = sellerRecords.filter(
            (record) => Number(record.orderId) === orderId,
        );
        expect(persistedSettlement).toHaveLength(1);
        expect(persistedSettlement[0]).toMatchObject({
            accountType: "BUSINESS",
            tradeType: "INCOME_BUSINESS",
        });
        expect(Number(persistedSettlement[0].amount)).toBeCloseTo(settlementAmount, 2);

        await assertNoVisibleError(page);
        await captureDomainEEvidence(
            page,
            testInfo,
            "UC23",
            "uc23-business-settlement-after-refresh",
        );
    });
});

function isFinanceResponse(path: string, method: string) {
    return (response: Response) =>
        response.url().includes(path) && response.request().method() === method;
}

async function financeDashboard(request: APIRequestContext, token: string): Promise<Dashboard> {
    return expectBusinessSuccess<Dashboard>(await request.get("/api/finance/dashboard", {
        headers: bearer(token),
    }));
}

async function walletRecords(request: APIRequestContext, token: string): Promise<FinanceRecord[]> {
    return expectBusinessSuccess<FinanceRecord[]>(await request.get(
        "/api/finance/my-wallet/records",
        { headers: bearer(token) },
    ));
}

async function businessRecords(request: APIRequestContext, token: string): Promise<FinanceRecord[]> {
    return expectBusinessSuccess<FinanceRecord[]>(await request.get(
        "/api/finance/business/records",
        { headers: bearer(token) },
    ));
}
