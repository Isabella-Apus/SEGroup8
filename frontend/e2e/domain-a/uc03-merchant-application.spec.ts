import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import {
    adminAccount,
    apiGet,
    apiPost,
    loginApi,
    registerAccount,
    uniqueAccount,
} from "../helpers/domain-a";

function applicationPayload(storeName: string) {
    return {
        storeName,
        categoryId: 1,
        idCardNo: "110101199001011234",
        bankCardNo: "6222021234567890123",
        licenseImg: "/license.png",
        warehouseAddr: "Guangdong Shenzhen Nanshan Road",
        warehouseProvince: "Guangdong",
        warehouseCity: "Shenzhen",
        warehouseDetail: "Nanshan Road",
        contactName: "Applicant",
        contactPhone: "13800138000",
    };
}

test.describe("@DOMAIN_A @UC03 real merchant application flow", () => {
    test("submit, review, role/shop upgrade and rejection persistence", async ({ page, request }) => {
        const admin = await loginApi(request, adminAccount());
        expect(admin.body.code).toBe(0);
        const adminToken = admin.account.token!;

        const applicant = await registerAccount(request, uniqueAccount("e2e-uc03-approved"));
        const applicantLogin = await loginApi(request, applicant);
        const applicantToken = applicantLogin.account.token!;
        await login(page, applicant);

        expect((await apiPost(request, "/api/user/merchant-application", applicantToken,
            applicationPayload("E2E Approved Shop"))).code).toBe(0);
        const pending = await apiGet(request, "/api/user/merchant-application/me", applicantToken);
        expect(pending.data.status).toBe(0);
        expect((await apiGet(request, "/api/admin/merchant-applications?status=0", adminToken)).code).toBe(0);

        const profile = await apiGet(request, "/api/user/profile", applicantToken);
        const applicationId = pending.data.id;
        expect((await apiPost(request, `/api/admin/merchant-applications/${applicationId}/approve`, adminToken)).code).toBe(0);
        const approvedProfile = await apiGet(request, "/api/user/profile", applicantToken);
        expect(approvedProfile.data.role).toBe("OFFICIAL_SELLER");
        expect(approvedProfile.data.shopName).toBe("E2E Approved Shop");
        expect(profile.data.role).toBe("USER");

        await page.goto("/profile");
        await page.reload();
        const refreshedApprovedProfile = await apiGet(request, "/api/user/profile", applicantToken);
        expect(refreshedApprovedProfile.data.role).toBe("OFFICIAL_SELLER");
        expect(refreshedApprovedProfile.data.shopName).toBe("E2E Approved Shop");
        await expect(page.locator(".page-card .el-form-item").nth(4).locator("input"))
            .toHaveValue("OFFICIAL_SELLER");

        const rejected = await registerAccount(request, uniqueAccount("e2e-uc03-rejected"));
        const rejectedLogin = await loginApi(request, rejected);
        const rejectedToken = rejectedLogin.account.token!;
        expect((await apiPost(request, "/api/user/merchant-application", rejectedToken,
            applicationPayload("E2E Rejected Shop"))).code).toBe(0);
        const rejectedApplication = await apiGet(request, "/api/user/merchant-application/me", rejectedToken);
        expect((await apiPost(request, `/api/admin/merchant-applications/${rejectedApplication.data.id}/reject`, adminToken,
            { rejectReason: "license is incomplete" })).code).toBe(0);
        const rejectedAfter = await apiGet(request, "/api/user/merchant-application/me", rejectedToken);
        expect(rejectedAfter.data.status).toBe(2);
        expect(rejectedAfter.data.rejectReason).toBe("license is incomplete");
        expect((await apiGet(request, "/api/user/profile", rejectedToken)).data.role).toBe("USER");
    });
});
