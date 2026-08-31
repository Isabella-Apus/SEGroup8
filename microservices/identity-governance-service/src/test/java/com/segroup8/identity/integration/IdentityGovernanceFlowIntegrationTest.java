package com.segroup8.identity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.segroup8.identity.support.IdentityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_A")
class IdentityGovernanceFlowIntegrationTest extends IdentityTestSupport {
    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    @Tag("UC02")
    void profileAndAddressOwnershipFlowUsesRealRepositoryAndMigration() throws Exception {
        register("owner");
        register("other");
        Login owner = login("owner", "User12345");
        Login other = login("other", "User12345");
        Map<String, Object> address = Map.of("receiverName", "Receiver", "receiverPhone", "13800138000",
                "province", "Guangdong", "city", "Shenzhen", "detailAddress", "Nanshan Road", "isDefault", 1);

        mvc.perform(put("/api/user/profile").header("Authorization", bearer(owner.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(Map.of("nickname", "Updated"))))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/user/addresses").header("Authorization", bearer(owner.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(address)))
                .andExpect(jsonPath("$.code").value(0));
        long addressId = db.queryForObject("SELECT id FROM address WHERE user_id=?", Long.class, owner.userId());
        mvc.perform(put("/api/user/addresses/{id}", addressId).header("Authorization", bearer(other.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(address)))
                .andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/user/addresses/{id}", addressId).header("Authorization", bearer(owner.token())))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Tag("UC03")
    void merchantApprovalUpdatesRoleAndPersistsOutboxInOneTransaction() throws Exception {
        register("merchant");
        Login merchant = login("merchant", "User12345");
        Login admin = login("admin", "admin123");
        Map<String, Object> request = Map.ofEntries(Map.entry("storeName", "Course Shop"),
                Map.entry("categoryId", 1), Map.entry("idCardNo", "110101199001011234"),
                Map.entry("bankCardNo", "6222021234567890123"), Map.entry("licenseImg", "/license.png"),
                Map.entry("warehouseAddr", "Nanshan Road"), Map.entry("warehouseProvince", "Guangdong"),
                Map.entry("warehouseCity", "Shenzhen"), Map.entry("warehouseDetail", "Nanshan Road"),
                Map.entry("contactName", "Applicant"), Map.entry("contactPhone", "13800138000"));
        mvc.perform(post("/api/user/merchant-application").header("Authorization", bearer(merchant.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(request)))
                .andExpect(jsonPath("$.code").value(0));
        long applicationId = db.queryForObject("SELECT id FROM merchant_application WHERE user_id=?", Long.class,
                merchant.userId());
        mvc.perform(post("/api/admin/merchant-applications/{id}/approve", applicationId)
                        .header("Authorization", bearer(admin.token())))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(db.queryForObject("SELECT role FROM `user` WHERE id=?", String.class, merchant.userId()))
                .isEqualTo("OFFICIAL_SELLER");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE event_type='MerchantApproved.v1'",
                Long.class)).isEqualTo(1);
    }

    @Test
    @Tag("UC04")
    void banUnbanChangesLoginAvailabilityAndWritesAudit() throws Exception {
        register("governed");
        Login governed = login("governed", "User12345");
        Login admin = login("admin", "admin123");
        mvc.perform(put("/api/admin/users/{id}/ban", governed.userId())
                        .header("Authorization", bearer(admin.token())))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(login("governed", "User12345").code()).isEqualTo(403);
        mvc.perform(put("/api/admin/users/{id}/unban", governed.userId())
                        .header("Authorization", bearer(admin.token())))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(login("governed", "User12345").code()).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM admin_audit_log WHERE target_id=?", Long.class,
                governed.userId())).isEqualTo(2);
    }

    @Test
    @Tag("UC05")
    void reportBlockCreditAuditCoversSuccessAndIllegalState() throws Exception {
        register("reporter");
        register("target");
        Login reporter = login("reporter", "User12345");
        Login target = login("target", "User12345");
        Login admin = login("admin", "admin123");

        mvc.perform(post("/api/report-block/report").header("Authorization", bearer(reporter.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(Map.of("reportedId", target.userId(),
                                "reasonType", "FRAUD", "reasonDesc", "evidence", "tradeContext", "SH_SELLER"))))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/report-block/block").header("Authorization", bearer(reporter.token()))
                        .contentType("application/json").content(json.writeValueAsBytes(Map.of("targetUserId", target.userId()))))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/report-block/block/blocked-by/{id}", reporter.userId())
                        .header("Authorization", bearer(target.token())))
                .andExpect(jsonPath("$.data").value(true));
        long reportId = db.queryForObject("SELECT id FROM user_report", Long.class);
        byte[] audit = json.writeValueAsBytes(Map.of("reportId", reportId, "decision", 1,
                "adminRemark", "confirmed", "customDelta", 20));
        mvc.perform(post("/api/admin/reports/audit").header("Authorization", bearer(admin.token()))
                        .contentType("application/json").content(audit))
                .andExpect(jsonPath("$.code").value(0));
        String creditBody = mvc.perform(get("/api/credit/{id}", target.userId())
                        .header("Authorization", bearer(target.token())))
                .andReturn().getResponse().getContentAsString();
        JsonNode credit = json.readTree(creditBody);
        assertThat(credit.path("data").path("buyerScore").asInt()).isEqualTo(80);
        mvc.perform(post("/api/admin/reports/audit").header("Authorization", bearer(admin.token()))
                        .contentType("application/json").content(audit))
                .andExpect(jsonPath("$.code").value(400));
    }
}
