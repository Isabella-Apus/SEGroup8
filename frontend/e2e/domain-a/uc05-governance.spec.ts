import { test, expect } from "../fixtures";

type ApiPayload = {
    code?: number;
    message?: string;
    data?: any;
};

async function apiJson(
    page: any,
    method: string,
    path: string,
    token?: string,
    body?: unknown,
): Promise<ApiPayload> {
    const response = await page.request.fetch(path, {
        method,
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        data: body,
    });
    expect(response.ok()).toBeTruthy();
    return response.json();
}

async function register(page: any, username: string, nickname: string) {
    const payload = await apiJson(page, "POST", "/api/auth/register", undefined, {
        username,
        password: "User12345",
        nickname,
    });
    expect(payload.code).toBe(0);
}

async function login(page: any, username: string, password: string) {
    const payload = await apiJson(page, "POST", "/api/auth/login", undefined, {
        username,
        password,
    });
    expect(payload.code).toBe(0);
    return {
        token: payload.data.token as string,
        userId: Number(payload.data.user.id),
    };
}

async function useToken(page: any, token: string) {
    await page.goto("/login");
    await page.evaluate((value) => localStorage.setItem("segroup8_token", value), token);
}

test.describe("@DOMAIN_A @UC05 real report block credit governance", () => {
    test("report, bilateral block state, credit audit and refresh persistence", async ({ page }) => {
        const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
        const reporterUsername = `e2e-uc05-reporter-${suffix}`;
        const targetUsername = `e2e-uc05-target-${suffix}`;

        await register(page, reporterUsername, "E2E Reporter");
        await register(page, targetUsername, "E2E Target");

        const reporter = await login(page, reporterUsername, "User12345");
        const target = await login(page, targetUsername, "User12345");
        const admin = await login(
            page,
            process.env.E2E_ADMIN_USERNAME || "admin",
            process.env.E2E_ADMIN_PASSWORD || "admin123",
        );

        let payload = await apiJson(page, "POST", "/api/report-block/report", reporter.token, {
            reportedId: target.userId,
            reasonType: "FRAUD",
            reasonDesc: "E2E invalid trade evidence",
            tradeContext: "SH_SELLER",
        });
        expect(payload.code).toBe(0);

        payload = await apiJson(page, "GET", "/api/report-block/report/my", reporter.token);
        expect(payload.code).toBe(0);
        expect(payload.data.records).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ reportedId: target.userId, reasonType: "FRAUD" }),
            ]),
        );

        payload = await apiJson(page, "POST", "/api/report-block/block", reporter.token, {
            targetUserId: target.userId,
        });
        expect(payload.code).toBe(0);
        payload = await apiJson(page, "GET", `/api/report-block/block/check/${target.userId}`, reporter.token);
        expect(payload.data).toBe(true);
        payload = await apiJson(page, "GET", `/api/report-block/block/blocked-by/${reporter.userId}`, target.token);
        expect(payload.data).toBe(true);
        payload = await apiJson(page, "GET", `/api/report-block/block/check/${reporter.userId}`, target.token);
        expect(payload.data).toBe(false);

        payload = await apiJson(page, "POST", "/api/report-block/block", reporter.token, {
            targetUserId: target.userId,
        });
        expect(payload.code).toBe(400);
        payload = await apiJson(page, "DELETE", `/api/report-block/block/${target.userId}`, reporter.token);
        expect(payload.code).toBe(0);
        payload = await apiJson(page, "DELETE", `/api/report-block/block/${target.userId}`, reporter.token);
        expect(payload.code).toBe(400);

        payload = await apiJson(page, "GET", "/api/admin/reports", admin.token);
        expect(payload.code).toBe(0);
        const report = payload.data.records.find(
            (item: any) => Number(item.reporterId) === reporter.userId
                && Number(item.reportedId) === target.userId,
        );
        expect(report).toBeTruthy();

        payload = await apiJson(page, "POST", "/api/admin/reports/audit", admin.token, {
            reportId: report.id,
            decision: 1,
            adminRemark: "E2E confirmed",
            customDelta: 20,
        });
        expect(payload.code).toBe(0);

        payload = await apiJson(page, "GET", `/api/credit/${target.userId}`, target.token);
        expect(payload.code).toBe(0);
        expect(payload.data.buyerScore).toBe(80);

        payload = await apiJson(page, "POST", "/api/admin/reports/audit", admin.token, {
            reportId: report.id,
            decision: 1,
        });
        expect(payload.code).toBe(400);
        payload = await apiJson(page, "POST", "/api/report-block/report", reporter.token, {
            reportedId: reporter.userId,
            reasonType: "FRAUD",
        });
        expect(payload.code).toBe(400);
        payload = await apiJson(page, "POST", "/api/report-block/block", reporter.token, {
            targetUserId: reporter.userId,
        });
        expect(payload.code).toBe(400);
        payload = await apiJson(page, "GET", "/api/admin/reports", target.token);
        expect(payload.code).toBe(403);

        await useToken(page, reporter.token);
        await page.goto("/credit");
        await expect(page.getByText("Credit Center")).toBeVisible();
        await expect(page.getByText("我的举报记录")).toBeVisible();
        await page.reload();
        await expect(page.getByText("E2E invalid trade evidence")).toBeVisible();

        await useToken(page, target.token);
        await page.goto("/credit");
        await expect(page.locator(".score-card").first().locator("strong")).toHaveText("80");
        await page.reload();
        await expect(page.locator(".score-card").first().locator("strong")).toHaveText("80");
    });
});
