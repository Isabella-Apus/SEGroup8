import { test, expect } from "../fixtures";
import { login, logout } from "../helpers/auth";
import { waitForApiResponse, assertNoVisibleError } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC15 review flow", () => {
    test("buyer reviews, seller replies, buyer follows up and state persists", async ({ page }, testInfo) => {
        const buyer = { username: process.env.E2E_USERNAME || "user", password: process.env.E2E_PASSWORD || "user123", role: "USER" as const };
        const seller = { username: process.env.E2E_SELLER_USERNAME || "seller", password: process.env.E2E_SELLER_PASSWORD || "seller123", role: "OFFICIAL_SELLER" as const };

        await login(page, buyer);
        let listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const card = page.locator(".order-card").filter({ hasText: "UC15-E2E-REVIEW-FLOW" });
        await expect(card).toBeVisible();
        await card.getByRole("button", { name: "\u53bb\u8bc4\u4ef7" }).click();
        await page.waitForURL(/\/order\/15001/);
        await expect(page.locator(".el-dialog:visible")).toBeVisible();
        await page.locator(".el-dialog:visible textarea").fill("\u9996\u8bc4\u4f53\u9a8c\u5f88\u597d");
        const reviewPromise = waitForApiResponse(page, "/api/order/15001/review/items", 200, "POST");
        await page.getByRole("button", { name: "\u63d0\u4ea4\u8bc4\u4ef7" }).click();
        expect((await reviewPromise).status()).toBe(200);

        await logout(page);
        await login(page, seller);
        const sellerReviewsPromise = waitForApiResponse(page, "/api/review/seller/list");
        await page.goto("/merchant/reviews");
        await sellerReviewsPromise;
        const reviewCard = page.locator(".review-card").filter({ hasText: "UC15-E2E-REVIEW-FLOW" }).first();
        await expect(reviewCard).toBeVisible();
        await reviewCard.getByRole("button", { name: "\u56de\u590d" }).click();
        await page.locator(".el-dialog:visible textarea").fill("\u611f\u8c22\u60a8\u7684\u53cd\u9988");
        const replyPromise = waitForApiResponse(page, "/api/review/", 200, "POST");
        await page.getByRole("button", { name: "\u63d0\u4ea4" }).last().click();
        expect((await replyPromise).status()).toBe(200);

        await logout(page);
        await login(page, buyer);
        const myReviewsPromise = waitForApiResponse(page, "/api/review/my");
        await page.goto("/my-reviews");
        await myReviewsPromise;
        const myCard = page.locator(".review-card").filter({ hasText: "UC15-E2E-REVIEW-FLOW" }).first();
        await expect(myCard).toBeVisible();
        await myCard.getByRole("button", { name: "\u8ffd\u52a0\u8bc4\u4ef7" }).click();
        await page.locator(".el-dialog:visible textarea").fill("\u8ffd\u8bc4\u4f7f\u7528\u4f53\u9a8c\u4f9d\u7136\u5f88\u597d");
        const followupPromise = waitForApiResponse(page, "/api/review/followup", 200, "POST");
        await page.getByRole("button", { name: "\u63d0\u4ea4" }).last().click();
        expect((await followupPromise).status()).toBe(200);

        await page.reload();
        await expect(page.locator(".review-card").filter({ hasText: "\u8ffd\u8bc4\u4f7f\u7528\u4f53\u9a8c\u4f9d\u7136\u5f88\u597d" })).toBeVisible();
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc15-review-persisted");
    });
});
