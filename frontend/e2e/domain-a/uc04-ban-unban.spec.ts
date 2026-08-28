import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import {
    adminAccount,
    apiGet,
    apiPut,
    loginApi,
    registerAccount,
    uniqueAccount,
} from "../helpers/domain-a";

test.describe("@DOMAIN_A @UC04 real ban and unban flow", () => {
    test("ban blocks login, unban restores login and audit is queryable", async ({ page, request }) => {
        const user = await registerAccount(request, uniqueAccount("e2e-uc04"));
        const userLogin = await loginApi(request, user);
        const userToken = userLogin.account.token!;
        await login(page, user);
        await page.goto("/profile");
        await expect(page.locator("body")).toContainText(user.username);

        const admin = await loginApi(request, adminAccount());
        expect(admin.body.code).toBe(0);
        const adminToken = admin.account.token!;
        const userId = userLogin.account.id;
        expect((await apiPut(request, `/api/admin/users/${userId}/ban`, adminToken)).code).toBe(0);
        expect((await loginApi(request, user)).body.code).toBe(403);

        expect((await apiPut(request, `/api/admin/users/${userId}/unban`, adminToken)).code).toBe(0);
        const recovered = await loginApi(request, user);
        expect(recovered.body.code).toBe(0);
        expect((await apiPut(request, `/api/admin/users/${userId}/unban`, adminToken)).code).toBe(0);
        expect((await apiPut(request, `/api/admin/users/${userId}/ban`, userToken)).code).toBe(403);
        expect((await apiPut(request, "/api/admin/users/1/ban", adminToken)).code).toBe(400);

        expect((await apiGet(request, "/api/admin/audit-logs?targetType=USER", adminToken)).code).toBe(0);
        await page.goto("/login");
        await login(page, user);
        await page.goto("/profile");
        await page.reload();
        await expect(page.locator("body")).toContainText(user.username);
    });
});
