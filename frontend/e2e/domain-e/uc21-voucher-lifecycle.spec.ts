import { test, expect } from "../fixtures";
import type { Locator } from "@playwright/test";
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
    const date = new Date(Date.now() + offsetDays * 86_400_000);
    const pad = (value: number) => String(value).padStart(2, "0");
    return [
        date.getFullYear(),
        "-",
        pad(date.getMonth() + 1),
        "-",
        pad(date.getDate()),
        " ",
        pad(date.getHours()),
        ":",
        pad(date.getMinutes()),
    ].join("");
}

async function fillDateTime(scope: Locator, placeholder: string, value: string) {
    const input = scope.getByPlaceholder(placeholder, { exact: true });
    await input.click();
    await input.fill(value);
    await input.press("Tab");
}

test.describe("@DOMAIN_E @UC21 voucher lifecycle", () => {
    test.describe.configure({ timeout: 90_000 });

    test("seller creates, edits and closes a voucher through the real UI", async ({
        page,
        request,
    }, testInfo) => {
        await loginAsDomainE(page, "OFFICIAL_SELLER");

        const originalName = uniqueName("UC21-E2E");
        const editedName = `${originalName}-edited`;
        const listResponse = page.waitForResponse(
            (response) => response.url().includes("/api/voucher/seller/list")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/merchant/vouchers");
        await expectBusinessSuccess(await listResponse);
        await expect(page.getByRole("heading", { name: "优惠券管理" })).toBeVisible();

        await page.getByRole("button", { name: "+ 创建优惠券", exact: true }).click();
        const createDialog = page.getByRole("dialog", { name: "创建优惠券" });
        await createDialog.getByPlaceholder("例如：新人专享券").fill(originalName);
        await createDialog.locator(".el-form-item").filter({ hasText: "优惠金额" })
            .getByRole("spinbutton").fill("10");
        await createDialog.locator(".el-form-item").filter({ hasText: "发放总量" })
            .getByRole("spinbutton").fill("50");
        await fillDateTime(createDialog, "领取开始时间", localDateTime(1));
        await fillDateTime(createDialog, "领取结束时间", localDateTime(2));
        await fillDateTime(createDialog, "开始时间", localDateTime(1));
        await fillDateTime(createDialog, "结束时间", localDateTime(7));

        const createResponse = page.waitForResponse(
            (response) => response.url().endsWith("/api/voucher/seller")
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await createDialog.getByRole("button", { name: "创建", exact: true }).click();
        const created = await expectBusinessSuccess<{ id: number }>(await createResponse);
        const voucherId = Number(created.id);
        expect(voucherId).toBeGreaterThan(0);

        const voucherRow = () => page.locator(".el-table__row").filter({ hasText: originalName });
        await expect(voucherRow()).toBeVisible();
        await voucherRow().getByRole("button", { name: "编辑", exact: true }).click();
        const editDialog = page.getByRole("dialog", { name: "编辑优惠券" });
        await editDialog.getByPlaceholder("例如：新人专享券").fill(editedName);
        const updateResponse = page.waitForResponse(
            (response) => response.url().endsWith(`/api/voucher/seller/${voucherId}`)
                && response.request().method() === "PUT",
            { timeout: responseTimeout },
        );
        await editDialog.getByRole("button", { name: "保存", exact: true }).click();
        const updated = await expectBusinessSuccess<any>(await updateResponse);
        expect(updated.name).toBe(editedName);

        const editedRow = () => page.locator(".el-table__row").filter({ hasText: editedName });
        await expect(editedRow()).toBeVisible();
        const closeResponse = page.waitForResponse(
            (response) => response.url().endsWith(`/api/voucher/seller/${voucherId}/close`)
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await editedRow().getByRole("button", { name: "关闭", exact: true }).click();
        const messageBox = page.locator(".el-message-box");
        await expect(messageBox).toBeVisible();
        await messageBox.getByRole("button", { name: /^(确定|OK)$/ }).click();
        await expectBusinessSuccess(await closeResponse);
        await expect(editedRow()).toContainText("已关闭");

        await page.reload();
        await expect(editedRow()).toContainText("已关闭");
        await captureDomainEEvidence(page, testInfo, "UC21", "uc21-voucher-closed-after-refresh");

        const buyerToken = await domainEToken(request, "BUYER");
        const forbidden = await request.post("/api/voucher/admin", {
            headers: bearer(buyerToken),
            data: {
                name: uniqueName("UC21-forbidden"),
                type: 1,
                discountAmount: 10,
                minAmount: 0,
                noThreshold: true,
                totalCount: 10,
                grabStartTime: new Date(Date.now() + 86_400_000).toISOString(),
                grabEndTime: new Date(Date.now() + 172_800_000).toISOString(),
                startTime: new Date(Date.now() + 86_400_000).toISOString(),
                endTime: new Date(Date.now() + 604_800_000).toISOString(),
            },
        });
        const forbiddenPayload = await expectBusinessFailure(forbidden);
        expect(Number(forbiddenPayload.code)).toBe(403);
        await assertNoVisibleError(page);
    });
});
