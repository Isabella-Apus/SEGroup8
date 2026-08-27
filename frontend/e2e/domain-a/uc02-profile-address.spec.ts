import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import {
    apiDelete,
    apiGet,
    apiPost,
    apiPut,
    loginApi,
    registerAccount,
    uniqueAccount,
} from "../helpers/domain-a";

test.describe("UC02 real profile and address flow", () => {
    test("updates profile, maintains one default address and isolates ownership", async ({ page, request }) => {
        const owner = await registerAccount(request, uniqueAccount("e2e-uc02-owner"));
        const ownerLogin = await loginApi(request, owner);
        const ownerToken = ownerLogin.account.token!;
        await login(page, owner);

        expect((await apiPut(request, "/api/user/profile", ownerToken, {
            nickname: "UC02 refreshed",
            email: "uc02@example.com",
        })).code).toBe(0);
        expect((await apiPost(request, "/api/user/addresses", ownerToken, {
            receiverName: "First Receiver",
            receiverPhone: "13800138000",
            province: "Guangdong",
            city: "Shenzhen",
            detailAddress: "First Street",
            isDefault: 1,
        })).code).toBe(0);
        expect((await apiPost(request, "/api/user/addresses", ownerToken, {
            receiverName: "Second Receiver",
            receiverPhone: "13800138000",
            province: "Guangdong",
            city: "Shenzhen",
            detailAddress: "Second Street",
            isDefault: 1,
        })).code).toBe(0);

        const addresses = await apiGet(request, "/api/user/addresses", ownerToken);
        expect(addresses.data).toHaveLength(2);
        expect(addresses.data.filter((item: any) => Number(item.isDefault) === 1)).toHaveLength(1);
        const firstId = addresses.data.find((item: any) => item.receiverName === "Second Receiver").id;
        expect((await apiPut(request, `/api/user/addresses/${firstId}`, ownerToken, {
            receiverName: "Updated Receiver",
            receiverPhone: "13800138000",
            province: "Guangdong",
            city: "Shenzhen",
            detailAddress: "Updated Street",
            isDefault: 1,
        })).code).toBe(0);
        const refreshed = await apiGet(request, "/api/user/addresses", ownerToken);
        const updated = refreshed.data.find((item: any) => item.id === firstId);
        expect(updated.receiverName).toBe("Updated Receiver");

        await page.goto("/profile");
        await page.reload();
        await expect(page.locator("body")).toContainText("UC02 refreshed");
        await page.goto("/addresses");
        await page.reload();
        await expect(page.locator("body")).toContainText("Updated Receiver");
        await expect(page.locator("body")).toContainText("Updated Street");

        expect((await apiDelete(request, `/api/user/addresses/${firstId}`, ownerToken)).code).toBe(0);
        const afterDelete = await apiGet(request, "/api/user/addresses", ownerToken);
        expect(afterDelete.data.find((item: any) => item.id === firstId)).toBeUndefined();
        await page.reload();
        await expect(page.locator("body")).not.toContainText("Updated Receiver");
        await expect(page.locator("body")).not.toContainText("Updated Street");

        const other = await registerAccount(request, uniqueAccount("e2e-uc02-other"));
        const otherLogin = await loginApi(request, other);
        const otherToken = otherLogin.account.token!;
        expect((await apiPut(request, `/api/user/addresses/${firstId}`, otherToken, {
            receiverName: "Intruder",
            receiverPhone: "13800138000",
            province: "Guangdong",
            city: "Shenzhen",
            detailAddress: "Intruder Street",
            isDefault: 1,
        })).code).toBe(404);
        expect((await apiDelete(request, `/api/user/addresses/${firstId}`, otherToken)).code).toBe(404);
    });
});
