package com.segroup8.identity.contract;

import com.segroup8.identity.support.IdentityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("CONTRACT")
class PublicApiSecurityContractTest extends IdentityTestSupport {
    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    void productionActuatorDoesNotExposeFlywayDetails() throws Exception {
        String links = mvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(links).doesNotContain("flyway");
        mvc.perform(get("/actuator/flyway")).andExpect(status().isNotFound());
    }

    @Test
    void everyProtectedPublicPathRejectsAnonymousRequests() throws Exception {
        byte[] empty = json.writeValueAsBytes(Map.of());
        List<MockHttpServletRequestBuilder> requests = List.of(
                get("/api/user/profile"), put("/api/user/profile").contentType("application/json").content(empty),
                get("/api/user/me"), get("/api/user/search"), get("/api/user/addresses"),
                post("/api/user/addresses").contentType("application/json").content(empty),
                put("/api/user/addresses/1").contentType("application/json").content(empty),
                delete("/api/user/addresses/1"),
                post("/api/user/merchant-application").contentType("application/json").content(empty),
                get("/api/user/merchant-application/me"), get("/api/admin/merchant-applications"),
                post("/api/admin/merchant-applications/1/approve"),
                post("/api/admin/merchant-applications/1/reject").contentType("application/json").content(empty),
                get("/api/admin/users"), put("/api/admin/users/2/ban"), put("/api/admin/users/2/unban"),
                post("/api/report-block/report").contentType("application/json").content(empty),
                get("/api/report-block/report/my"),
                post("/api/report-block/block").contentType("application/json").content(empty),
                delete("/api/report-block/block/2"), get("/api/report-block/block/my"),
                get("/api/report-block/block/check/2"), get("/api/report-block/block/blocked-by/2"),
                get("/api/credit/me"), get("/api/credit/2"), get("/api/admin/reports"),
                post("/api/admin/reports/audit").contentType("application/json").content(empty),
                post("/api/admin/reports/credit-adjust").param("userId", "2").param("role", "BUYER").param("delta", "1"),
                get("/api/admin/audit-logs"));
        for (MockHttpServletRequestBuilder request : requests) {
            mvc.perform(request).andExpect(jsonPath("$.code").value(401));
        }
    }

    @Test
    void everyAdminPathRejectsOrdinaryUsers() throws Exception {
        register("ordinary");
        Login user = login("ordinary", "User12345");
        String authorization = bearer(user.token());
        byte[] reject = json.writeValueAsBytes(Map.of("rejectReason", "invalid"));
        byte[] audit = json.writeValueAsBytes(Map.of("reportId", 1, "decision", 2));
        List<MockHttpServletRequestBuilder> requests = List.of(
                get("/api/admin/merchant-applications"),
                post("/api/admin/merchant-applications/1/approve"),
                post("/api/admin/merchant-applications/1/reject").contentType("application/json").content(reject),
                get("/api/admin/users"), put("/api/admin/users/2/ban"), put("/api/admin/users/2/unban"),
                get("/api/admin/reports"),
                post("/api/admin/reports/audit").contentType("application/json").content(audit),
                post("/api/admin/reports/credit-adjust").param("userId", "2").param("role", "BUYER").param("delta", "1"),
                get("/api/admin/audit-logs"));
        for (MockHttpServletRequestBuilder request : requests) {
            mvc.perform(request.header("Authorization", authorization))
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Test
    void everyProtectedPublicPathRejectsADeletedAccountToken() throws Exception {
        register("deleted-account");
        Login deleted = login("deleted-account", "User12345");
        db.update("DELETE FROM `user` WHERE id=?", deleted.userId());
        String authorization = bearer(deleted.token());
        byte[] empty = json.writeValueAsBytes(Map.of());
        List<MockHttpServletRequestBuilder> requests = List.of(
                get("/api/user/profile"), put("/api/user/profile").contentType("application/json").content(empty),
                get("/api/user/me"), get("/api/user/search"), get("/api/user/addresses"),
                post("/api/user/addresses").contentType("application/json").content(empty),
                put("/api/user/addresses/1").contentType("application/json").content(empty),
                delete("/api/user/addresses/1"),
                post("/api/user/merchant-application").contentType("application/json").content(empty),
                get("/api/user/merchant-application/me"), get("/api/admin/merchant-applications"),
                post("/api/admin/merchant-applications/1/approve"),
                post("/api/admin/merchant-applications/1/reject").contentType("application/json").content(empty),
                get("/api/admin/users"), put("/api/admin/users/2/ban"), put("/api/admin/users/2/unban"),
                post("/api/report-block/report").contentType("application/json").content(empty),
                get("/api/report-block/report/my"),
                post("/api/report-block/block").contentType("application/json").content(empty),
                delete("/api/report-block/block/2"), get("/api/report-block/block/my"),
                get("/api/report-block/block/check/2"), get("/api/report-block/block/blocked-by/2"),
                get("/api/credit/me"), get("/api/credit/2"), get("/api/admin/reports"),
                post("/api/admin/reports/audit").contentType("application/json").content(empty),
                post("/api/admin/reports/credit-adjust").param("userId", "2").param("role", "BUYER").param("delta", "1"),
                get("/api/admin/audit-logs"));
        for (MockHttpServletRequestBuilder request : requests) {
            mvc.perform(request.header("Authorization", authorization))
                    .andExpect(jsonPath("$.code").value(401));
        }
    }
}
