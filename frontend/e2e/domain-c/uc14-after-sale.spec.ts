import { test, expect } from "../fixtures";
import { login, logout } from "../helpers/auth";
import { waitForApiResponse, assertNoVisibleError } from "../helpers/http";
import { captureEvidence } from "./support/evidence";

test.describe("@DOMAIN_C @UC14 after-sale refund", () => {
    test("buyer applies and seller approves, then admin arbitrates", async ({ page }, testInfo) => {
        const buyer = { username: process.env.E2E_USERNAME || "user", password: process.env.E2E_PASSWORD || "user123", role: "USER" as const };
        const seller = { username: process.env.E2E_SELLER_USERNAME || "seller", password: process.env.E2E_SELLER_PASSWORD || "seller123", role: "OFFICIAL_SELLER" as const };
        const admin = { username: process.env.E2E_ADMIN_USERNAME || "admin", password: process.env.E2E_ADMIN_PASSWORD || "admin123", role: "ADMIN" as const };

        await login(page, buyer);
        let listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const sellerCard = page.locator(".order-card").filter({ hasText: "UC14-E2E-AFTER-SALE" });
        await expect(sellerCard).toBeVisible();
        await sellerCard.getByRole("button", { name: "\u7533\u8bf7\u9000\u8d27" }).click();
        await page.locator(".el-dialog:visible .el-select").click();
        await page.getByRole("option", { name: "\u8d28\u91cf\u95ee\u9898/\u635f\u574f" }).click();
        const applyPromise = waitForApiResponse(page, "/api/order/14001/refund", 200, "POST");
        await page.getByRole("button", { name: "\u63d0\u4ea4\u7533\u8bf7" }).click();
        expect((await applyPromise).status()).toBe(200);

        await logout(page);
        await login(page, seller);
        let sellerListPromise = waitForApiResponse(page, "/api/order/seller/list");
        await page.goto("/merchant/orders");
        await sellerListPromise;
        const sellerRow = page.locator("tr").filter({ hasText: "UC14-E2E-AFTER-SALE" });
        await expect(sellerRow).toBeVisible();
        const approvePromise = waitForApiResponse(page, "/api/order/14001/refund/approve", 200, "POST");
        await sellerRow.getByRole("button", { name: "\u540c\u610f\u9000\u8d27" }).click();
        await page.locator(".el-message-box__btns button").last().click();
        expect((await approvePromise).status()).toBe(200);

        await logout(page);
        await login(page, buyer);
        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        await expect(page.locator(".order-card").filter({ hasText: "UC14-E2E-AFTER-SALE" })).toContainText("\u5df2\u5173\u95ed");

        await logout(page);
        await login(page, buyer);
        listPromise = waitForApiResponse(page, "/api/order/list");
        await page.goto("/order");
        await listPromise;
        const adminCard = page.locator(".order-card").filter({ hasText: "UC14-E2E-ADMIN-ARBITRATION" });
        await expect(adminCard).toBeVisible();
        await adminCard.getByRole("button", { name: "\u7533\u8bf7\u9000\u8d27" }).click();
        await page.locator(".el-dialog:visible .el-select").click();
        await page.getByRole("option", { name: "\u5176\u4ed6" }).click();
        const adminApplyPromise = waitForApiResponse(page, "/api/order/14002/refund", 200, "POST");
        await page.getByRole("button", { name: "\u63d0\u4ea4\u7533\u8bf7" }).click();
        expect((await adminApplyPromise).status()).toBe(200);

        await logout(page);
        await login(page, admin);
        const adminListPromise = waitForApiResponse(page, "/api/admin/orders/list");
        await page.goto("/admin/orders");
        await adminListPromise;
        const adminRow = page.locator("tr").filter({ hasText: "UC14-E2E-ADMIN-ARBITRATION" });
        await expect(adminRow).toBeVisible();
        await adminRow.locator("button").first().click();
        const adminDrawer = page.locator(".el-drawer:visible");
        await expect(adminDrawer.locator(".refund-actions__title")).toBeVisible();
        const adminApprovePromise = waitForApiResponse(page, "/api/admin/orders/14002/refund/approve", 200, "POST");
        await adminDrawer.getByRole("button", { name: "\u540c\u610f\u9000\u8d27" }).click();
        await page.locator(".el-message-box__btns button").last().click();
        expect((await adminApprovePromise).status()).toBe(200);

        await page.reload();
        await expect(page.locator("tr").filter({ hasText: "UC14-E2E-ADMIN-ARBITRATION" })).toContainText("\u5df2\u9000\u6b3e");
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc14-after-sale-persisted");
    });
});
