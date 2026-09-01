package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.entity.UserBlock;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.service.ReportBlockService;
import com.segroup8.platform.vo.CreditScoreVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC05")
class ReportBlockControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private ReportBlockService reportBlockService;
    @Mock
    private CreditService creditService;
    @Mock
    private AdminAuditLogService adminAuditLogService;
    @Mock
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ReportBlockController(reportBlockService),
                        new CreditController(creditService),
                        new AdminReportController(reportBlockService, adminAuditLogService, creditService, userMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void submitReport_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/report-block/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":2,\"reasonType\":\"FRAUD\",\"reasonDesc\":\"Suspicious trade\",\"tradeContext\":\"SHOP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(reportBlockService).submitReport(any());
    }

    @Test
    void block_shouldRejectMissingTarget() throws Exception {
        mockMvc.perform(post("/api/report-block/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void block_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/report-block/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(reportBlockService).blockUser(any());
    }

    @Test
    void reportAndBlockQueries_shouldReturnSuccess() throws Exception {
        when(reportBlockService.myReports(1, 10)).thenReturn(new Page<>(1, 10));
        when(reportBlockService.myBlockList()).thenReturn(List.of(new UserBlock()));
        when(reportBlockService.isBlocking(3L)).thenReturn(true);
        when(reportBlockService.isBlockedBy(3L)).thenReturn(false);

        mockMvc.perform(get("/api/report-block/report/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
        mockMvc.perform(get("/api/report-block/block/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/report-block/block/check/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/report-block/block/blocked-by/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(reportBlockService).myReports(1, 10);
        verify(reportBlockService).myBlockList();
        verify(reportBlockService).isBlocking(3L);
        verify(reportBlockService).isBlockedBy(3L);
    }

    @Test
    void unblock_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/report-block/block/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(reportBlockService).unblockUser(any());
    }

    @Test
    void myCredit_shouldReturnSuccess() throws Exception {
        when(creditService.getMyCredit()).thenReturn(new CreditScoreVO());

        mockMvc.perform(get("/api/credit/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void userCredit_shouldReturnSuccess() throws Exception {
        when(creditService.getCreditInfo(3L)).thenReturn(new CreditScoreVO());

        mockMvc.perform(get("/api/credit/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());

        verify(creditService).getCreditInfo(3L);
    }

    @Test
    void adminListReports_shouldReturnSuccess() throws Exception {
        setAdminContext();
        when(reportBlockService.adminListReports(1, 10, null, null)).thenReturn(new Page<>(1, 10));

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());

        verify(reportBlockService).adminListReports(1, 10, null, null);
    }

    @Test
    void creditAdjust_shouldReturnSuccessAndDelegate() throws Exception {
        setAdminContext();

        mockMvc.perform(post("/api/admin/reports/credit-adjust")
                        .param("userId", "3")
                        .param("role", "BUYER")
                        .param("delta", "-5")
                        .param("remark", "manual correction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(creditService).adminAdjust(3L, "BUYER", -5, "manual correction", 1L);
        verify(adminAuditLogService).record(eq("CREDIT_ADJUST"), eq("USER"), eq(3L), anyString());
    }

    @Test
    void auditReport_shouldRequireAdmin() throws Exception {
        setAdminContext();

        mockMvc.perform(post("/api/admin/reports/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":10,\"decision\":1,\"adminRemark\":\"verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(reportBlockService).adminAuditReport(any());
        verify(adminAuditLogService).record(eq("REPORT_UPHELD"), eq("USER_REPORT"), eq(10L), anyString());
    }

    @Test
    void adminEndpoint_shouldRejectNonAdmin() throws Exception {
        UserContext.setUserId(2L);
        User user = new User();
        user.setId(2L);
        user.setRole(RoleEnum.USER.name());
        when(userMapper.selectById(2L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void setAdminContext() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(admin);
    }
}
