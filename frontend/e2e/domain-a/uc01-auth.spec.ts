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

test.describe("@DOMAIN_A @UC01 real auth and identity flow", () => {
    test("register, login, role boundary, ban and refresh persistence", async ({ page, request }) => {
        const user = await registerAccount(request, uniqueAccount("e2e-uc01"));
        const loggedIn = await loginApi(request, user);
        expect(loggedIn.body.code).toBe(0);
        expect(loggedIn.account.token).toBeTruthy();

        await login(page, user);
        await page.goto("/profile");
        await expect(page.locator("body")).toContainText(user.username);
        await page.reload();
        await expect(page.locator("body")).toContainText(user.username);

        const userToken = loggedIn.account.token!;
        const admin = await loginApi(request, adminAccount());
        expect(admin.body.code).toBe(0);
        const adminToken = admin.account.token!;

        expect((await apiGet(request, "/api/admin/users", userToken)).code).toBe(403);
        expect((await apiGet(request, "/api/admin/users", adminToken)).code).toBe(0);

        expect((await apiPut(request, `/api/admin/users/${loggedIn.account.id}/ban`, adminToken)).code).toBe(0);
        const bannedLogin = await loginApi(request, user);
        expect(bannedLogin.body.code).toBe(403);
    });
});
