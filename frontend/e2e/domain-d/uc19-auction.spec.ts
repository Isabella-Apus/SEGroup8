import { expect, test } from "../fixtures";
import {
    apiToken,
    bearer,
    captureEvidence,
    expectBusinessSuccess,
    loginAsDomainD,
    uniqueName,
} from "./support";

const responseTimeout = 30_000;
const productImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='600'%3E%3Crect width='600' height='600' fill='%23dff8f1'/%3E%3C/svg%3E";

test.describe("@DOMAIN_D @UC19 secondhand auction", () => {
    test.describe.configure({ timeout: 150_000 });

    test("seller creates an auction, two buyers bid, and the winner receives one settled order", async ({
        browser,
        page,
        request,
    }, testInfo) => {
        const sellerToken = await apiToken(request, "seller");
        const bidderAToken = await apiToken(request, "buyer");
        const bidderBToken = await apiToken(request, "third");
        const productName = uniqueName("UC19-E2E");

        const recharge = async (token: string) => expectBusinessSuccess<any>(
            await request.post("/api/finance/recharge", {
                headers: bearer(token),
                data: { amount: 500, channel: "UC19_E2E" },
            }),
        );
        const bidderABefore = Number((await recharge(bidderAToken)).personalBalance);
        const bidderBBefore = Number((await recharge(bidderBToken)).personalBalance);

        const created = await expectBusinessSuccess<any>(
            await request.post("/api/secondhand/seller", {
                headers: bearer(sellerToken),
                data: {
                    name: productName,
                    cover: productImage,
                    images: [productImage],
                    description: "UC19 real Compose two-bidder auction fixture",
                    originPrice: 180,
                    salePrice: 50,
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

        await loginAsDomainD(page, "seller");
        await page.goto(`/secondhand/${productId}`);
        await expect(page.getByText(productName, { exact: true })).toBeVisible();
        await page.getByRole("button", { name: "发起拍卖", exact: true }).click();
        const createDialog = page.getByRole("dialog", { name: "发起拍卖" });
        await createDialog.locator(".el-form-item").filter({ hasText: "起拍价" })
            .getByRole("spinbutton").fill("50");
        await createDialog.locator(".el-form-item").filter({ hasText: "加价幅度" })
            .getByRole("spinbutton").fill("5");
        await createDialog.locator(".el-form-item").filter({ hasText: "拍卖时长" })
            .getByRole("spinbutton").fill("60");
        const createResponse = page.waitForResponse(
            (response) => response.url().endsWith("/api/secondhand/trade/auction")
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await createDialog.getByRole("button", { name: "确认发起", exact: true }).click();
        const auction = await expectBusinessSuccess<any>(await createResponse);
        const auctionId = Number(auction.id);
        expect(auctionId).toBeGreaterThan(0);
        expect(auction.status).toBe("ONGOING");
        await expect(page.getByText("拍卖情况", { exact: true })).toBeVisible();
        await expect(page.getByText("暂无出价", { exact: true }).first()).toBeVisible();
        await captureEvidence(page, testInfo, "uc19-seller-created-auction");

        const baseURL = String(testInfo.project.use.baseURL || "http://127.0.0.1:8088");
        const bidderAContext = await browser.newContext({ baseURL });
        const bidderBContext = await browser.newContext({ baseURL });
        const bidderAPage = await bidderAContext.newPage();
        const bidderBPage = await bidderBContext.newPage();
        try {
            await loginAsDomainD(bidderAPage, "buyer");
            await bidderAPage.goto(`/secondhand/${productId}`);
            await bidderAPage.getByRole("button", { name: "参与竞拍", exact: true }).click();
            const bidderADialog = bidderAPage.getByRole("dialog", { name: "参与竞拍" });
            await bidderADialog.getByRole("spinbutton").fill("50");
            const bidderAResponse = bidderAPage.waitForResponse(
                (response) => response.url().endsWith(`/api/secondhand/trade/auction/${auctionId}/bid`)
                    && response.request().method() === "POST",
                { timeout: responseTimeout },
            );
            await bidderADialog.getByRole("button", { name: "确认出价", exact: true }).click();
            const firstBid = await expectBusinessSuccess<any>(await bidderAResponse);
            expect(Number(firstBid.currentPrice)).toBe(50);
            expect(Number(firstBid.currentBidderUserId)).toBe(3);
            expect(Number(firstBid.bidCount)).toBe(1);
            await expect(bidderAPage.getByText("你目前是最高出价", { exact: false })).toBeVisible();

            await loginAsDomainD(bidderBPage, "third");
            await bidderBPage.goto(`/secondhand/${productId}`);
            await bidderBPage.getByRole("button", { name: "参与竞拍", exact: true }).click();
            const bidderBDialog = bidderBPage.getByRole("dialog", { name: "参与竞拍" });
            await bidderBDialog.getByRole("spinbutton").fill("60");
            const bidderBResponse = bidderBPage.waitForResponse(
                (response) => response.url().endsWith(`/api/secondhand/trade/auction/${auctionId}/bid`)
                    && response.request().method() === "POST",
                { timeout: responseTimeout },
            );
            await bidderBDialog.getByRole("button", { name: "确认出价", exact: true }).click();
            const secondBid = await expectBusinessSuccess<any>(await bidderBResponse);
            expect(Number(secondBid.currentPrice)).toBe(60);
            expect(Number(secondBid.currentBidderUserId)).toBe(4);
            expect(Number(secondBid.bidCount)).toBe(2);
            expect(secondBid.logs).toHaveLength(2);
            await expect(bidderBPage.getByText("你目前是最高出价", { exact: false })).toBeVisible();
            await captureEvidence(bidderBPage, testInfo, "uc19-bidder-b-leading");

            const bidderAFinance = await expectBusinessSuccess<any>(
                await request.get("/api/finance/dashboard", { headers: bearer(bidderAToken) }),
            );
            const bidderBFinance = await expectBusinessSuccess<any>(
                await request.get("/api/finance/dashboard", { headers: bearer(bidderBToken) }),
            );
            expect(Number(bidderAFinance.personalBalance)).toBeCloseTo(bidderABefore, 2);
            expect(Number(bidderBFinance.personalBalance)).toBeCloseTo(bidderBBefore - 60, 2);

            await page.goto("/secondhand/mine");
            const auctionCard = page.locator(".auction-card").filter({ hasText: productName });
            await expect(auctionCard).toBeVisible();
            await expect(auctionCard).toContainText("2 次出价");
            await expect(auctionCard).toContainText("Third Party User");
            await captureEvidence(page, testInfo, "uc19-seller-monitor-two-bids");

            const closeResponse = page.waitForResponse(
                (response) => response.url().endsWith(`/api/secondhand/trade/auction/${auctionId}/close`)
                    && response.request().method() === "POST",
                { timeout: responseTimeout },
            );
            await auctionCard.getByRole("button", { name: "提前结束", exact: true }).click();
            const finished = await expectBusinessSuccess<any>(await closeResponse);
            const orderId = Number(finished.settledOrderId);
            expect(finished.status).toBe("FINISHED");
            expect(orderId).toBeGreaterThan(0);

            const order = await expectBusinessSuccess<any>(
                await request.get(`/api/order/detail/${orderId}`, {
                    headers: bearer(bidderBToken),
                }),
            );
            expect(Number(order.orderStatus)).toBe(1);
            expect(Number(order.payStatus)).toBe(1);
            expect(Number(order.totalAmount)).toBe(60);
            expect(order.items).toEqual(expect.arrayContaining([
                expect.objectContaining({
                    productType: "SECONDHAND",
                    productId,
                    productName,
                    price: 60,
                }),
            ]));

            await bidderBPage.reload();
            await expect(bidderBPage.getByText("已成交", { exact: true }).first()).toBeVisible();
            await expect(bidderBPage.getByText("你已竞拍成功", { exact: false })).toBeVisible();
            await bidderBPage.goto("/secondhand/orders");
            await expect(bidderBPage.getByText(productName, { exact: true })).toBeVisible();
            await expect(bidderBPage.getByText("待发货", { exact: true }).first()).toBeVisible();
            await captureEvidence(bidderBPage, testInfo, "uc19-winner-paid-pending-shipment-order");
        } finally {
            await bidderAContext.close();
            await bidderBContext.close();
        }
    });
});
