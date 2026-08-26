package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.service.ReportBlockService;
import com.segroup8.platform.vo.CreditScoreVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
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
    void auditReport_shouldRequireAdmin() throws Exception {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(admin);

        mockMvc.perform(post("/api/admin/reports/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":10,\"decision\":1,\"adminRemark\":\"verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(reportBlockService).adminAuditReport(any());
        verify(adminAuditLogService).record(eq("REPORT_UPHELD"), eq("USER_REPORT"), eq(10L), anyString());
    }
}
