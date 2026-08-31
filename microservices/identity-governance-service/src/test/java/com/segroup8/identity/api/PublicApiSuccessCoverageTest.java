package com.segroup8.identity.api;

import com.segroup8.identity.support.IdentityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_A")
class PublicApiSuccessCoverageTest extends IdentityTestSupport {
    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    @Tag("UC02")
    void everyUserProfileAndAddressOperationHasASuccessAssertion() throws Exception {
        register("profile-owner");
        Login owner = login("profile-owner", "User12345");
        String authorization = bearer(owner.token());

        expectSuccess(get("/api/user/profile").header("Authorization", authorization));
        expectSuccess(get("/api/user/me").header("Authorization", authorization));
        expectSuccess(put("/api/user/profile").header("Authorization", authorization)
                .contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("nickname", "Updated Owner"))));
        expectSuccess(get("/api/user/search").param("keyword", "profile")
                .header("Authorization", authorization));
        expectSuccess(get("/api/user/addresses").header("Authorization", authorization));

        Map<String, Object> address = address("Nanshan Road");
        expectSuccess(post("/api/user/addresses").header("Authorization", authorization)
                .contentType("application/json").content(json.writeValueAsBytes(address)));
        long addressId = db.queryForObject("SELECT id FROM address WHERE user_id=?", Long.class, owner.userId());
        expectSuccess(put("/api/user/addresses/{id}", addressId).header("Authorization", authorization)
                .contentType("application/json").content(json.writeValueAsBytes(address("Futian Road"))));
        expectSuccess(delete("/api/user/addresses/{id}", addressId).header("Authorization", authorization));
    }

    @Test
    @Tag("UC03")
    @Tag("UC04")
    void everyMerchantAndUserAdministrationOperationHasASuccessAssertion() throws Exception {
        register("approved-merchant");
        register("rejected-merchant");
        Login approved = login("approved-merchant", "User12345");
        Login rejected = login("rejected-merchant", "User12345");
        Login admin = login("admin", "admin123");

        submitMerchant(approved, "Approved Shop");
        submitMerchant(rejected, "Rejected Shop");
        expectSuccess(get("/api/user/merchant-application/me")
                .header("Authorization", bearer(approved.token())));
        expectSuccess(get("/api/admin/merchant-applications")
                .header("Authorization", bearer(admin.token())));

        long approvedId = db.queryForObject(
                "SELECT id FROM merchant_application WHERE user_id=?", Long.class, approved.userId());
        long rejectedId = db.queryForObject(
                "SELECT id FROM merchant_application WHERE user_id=?", Long.class, rejected.userId());
        expectSuccess(post("/api/admin/merchant-applications/{id}/approve", approvedId)
                .header("Authorization", bearer(admin.token())));
        expectSuccess(post("/api/admin/merchant-applications/{id}/reject", rejectedId)
                .header("Authorization", bearer(admin.token())).contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("rejectReason", "材料不完整"))));

        expectSuccess(get("/api/admin/users").header("Authorization", bearer(admin.token())));
        expectSuccess(put("/api/admin/users/{id}/ban", rejected.userId())
                .header("Authorization", bearer(admin.token())));
        expectSuccess(put("/api/admin/users/{id}/unban", rejected.userId())
                .header("Authorization", bearer(admin.token())));
        expectSuccess(get("/api/admin/audit-logs").header("Authorization", bearer(admin.token())));
    }

    @Test
    @Tag("UC05")
    void everyGovernanceAndCreditOperationHasASuccessAssertion() throws Exception {
        register("reporter-user");
        register("reported-user");
        Login reporter = login("reporter-user", "User12345");
        Login reported = login("reported-user", "User12345");
        Login admin = login("admin", "admin123");
        String reporterAuthorization = bearer(reporter.token());

        expectSuccess(post("/api/report-block/report").header("Authorization", reporterAuthorization)
                .contentType("application/json").content(json.writeValueAsBytes(Map.of(
                        "reportedId", reported.userId(), "reasonType", "FRAUD",
                        "reasonDesc", "evidence", "tradeContext", "SHOP"))));
        expectSuccess(get("/api/report-block/report/my").header("Authorization", reporterAuthorization));
        expectSuccess(post("/api/report-block/block").header("Authorization", reporterAuthorization)
                .contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("targetUserId", reported.userId()))));
        expectSuccess(get("/api/report-block/block/my").header("Authorization", reporterAuthorization));
        expectSuccess(get("/api/report-block/block/check/{id}", reported.userId())
                .header("Authorization", reporterAuthorization));
        expectSuccess(get("/api/report-block/block/blocked-by/{id}", reporter.userId())
                .header("Authorization", bearer(reported.token())));
        expectSuccess(delete("/api/report-block/block/{id}", reported.userId())
                .header("Authorization", reporterAuthorization));
        expectSuccess(get("/api/credit/me").header("Authorization", reporterAuthorization));
        expectSuccess(get("/api/credit/{id}", reported.userId())
                .header("Authorization", reporterAuthorization));

        String adminAuthorization = bearer(admin.token());
        expectSuccess(get("/api/admin/reports").header("Authorization", adminAuthorization));
        long reportId = db.queryForObject("SELECT id FROM user_report", Long.class);
        expectSuccess(post("/api/admin/reports/audit").header("Authorization", adminAuthorization)
                .contentType("application/json").content(json.writeValueAsBytes(Map.of(
                        "reportId", reportId, "decision", 1, "adminRemark", "confirmed"))));
        expectSuccess(post("/api/admin/reports/credit-adjust").header("Authorization", adminAuthorization)
                .param("userId", String.valueOf(reported.userId())).param("role", "SELLER")
                .param("delta", "5").param("remark", "course verification"));
    }

    private void submitMerchant(Login applicant, String storeName) throws Exception {
        Map<String, Object> request = Map.ofEntries(Map.entry("storeName", storeName),
                Map.entry("categoryId", 1), Map.entry("idCardNo", "110101199001011234"),
                Map.entry("bankCardNo", "6222021234567890123"), Map.entry("licenseImg", "/license.png"),
                Map.entry("warehouseAddr", "Nanshan Road"), Map.entry("warehouseProvince", "Guangdong"),
                Map.entry("warehouseCity", "Shenzhen"), Map.entry("warehouseDetail", "Nanshan Road"),
                Map.entry("contactName", "Applicant"), Map.entry("contactPhone", "13800138000"));
        expectSuccess(post("/api/user/merchant-application").header("Authorization", bearer(applicant.token()))
                .contentType("application/json").content(json.writeValueAsBytes(request)));
    }

    private Map<String, Object> address(String detail) {
        return Map.of("receiverName", "Receiver", "receiverPhone", "13800138000",
                "province", "Guangdong", "city", "Shenzhen", "detailAddress", detail, "isDefault", 1);
    }

    private void expectSuccess(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mvc.perform(request).andExpect(jsonPath("$.code").value(0));
    }
}
