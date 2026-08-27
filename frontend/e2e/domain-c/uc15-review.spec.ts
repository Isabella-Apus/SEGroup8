import { test, expect } from "../fixtures";
import { getTestAccount, login, logout } from "../helpers/auth";
import { waitForApiResponse, assertNoVisibleError } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC15 review flow", () => {
    test("buyer reviews, seller replies, buyer follows up and state persists", async ({ page }, testInfo) => {
        const buyer = getTestAccount("USER");
        const seller = getTestAccount("OFFICIAL_SELLER");
        const orderNo = "UC15-E2E-REVIEW-FLOW";
        const replyContent = "感谢您的反馈";
        const followupContent = "追评使用体验依然很好";

        // A retry resumes from the persisted state left by an interrupted attempt.
        await login(page, buyer);
        let listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const card = page.locator(".order-card").filter({ hasText: orderNo });
        await expect(card).toBeVisible();

        const reviewButton = card.getByRole("button", { name: "去评价" });
        if (await reviewButton.count()) {
            await reviewButton.click();
            await page.waitForURL(/\/order\/15001/);
            await expect(page.locator(".el-dialog:visible")).toBeVisible();
            await page.locator(".el-dialog:visible textarea").fill("首评体验很好");
            const reviewPromise = waitForApiResponse(page, "/api/order/15001/review/items", 200, "POST");
            await page.getByRole("button", { name: "提交评价" }).click();
            expect((await reviewPromise).status()).toBe(200);
        }

        await logout(page);
        await login(page, seller);
        const sellerReviewsPromise = waitForApiResponse(page, "/api/review/seller/list");
        await page.goto("/merchant/reviews");
        await sellerReviewsPromise;
        const reviewCard = page.locator(".review-card").filter({ hasText: orderNo }).first();
        await expect(reviewCard).toBeVisible();
        const replyButton = reviewCard.getByRole("button", { name: "回复" });
        if (await replyButton.count()) {
            await replyButton.click();
            await page.locator(".el-dialog:visible textarea").fill(replyContent);
            const replyPromise = waitForApiResponse(page, "/api/review/", 200, "POST");
            await page.getByRole("button", { name: "提交" }).last().click();
            expect((await replyPromise).status()).toBe(200);
        }

        await logout(page);
        await login(page, buyer);
        const myReviewsPromise = waitForApiResponse(page, "/api/review/my");
        await page.goto("/my-reviews");
        await myReviewsPromise;
        const myCard = page.locator(".review-card").filter({ hasText: orderNo }).first();
        await expect(myCard).toBeVisible();
        const followupButton = myCard.getByRole("button", { name: "追加评价" });
        if (await followupButton.count()) {
            await followupButton.click();
            await page.locator(".el-dialog:visible textarea").fill(followupContent);
            const followupPromise = waitForApiResponse(page, "/api/review/followup", 200, "POST");
            await page.getByRole("button", { name: "提交" }).last().click();
            expect((await followupPromise).status()).toBe(200);
        }

        await page.reload();
        const persistedReview = page.locator(".review-card").filter({ hasText: orderNo }).first();
        await expect(persistedReview).toContainText(replyContent);
        await expect(persistedReview).toContainText(followupContent);
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc15-review-persisted");

        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const completedOrder = page.locator(".order-card").filter({ hasText: orderNo });
        await expect(completedOrder).toContainText("已完成");
        await expect(completedOrder.getByRole("button", { name: "查看评价" })).toBeVisible();
    });
});
