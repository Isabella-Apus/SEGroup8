package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.PageVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC03")
class MerchantApplicationControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;
    @Mock
    private MerchantApplicationService merchantApplicationService;
    @Mock
    private AdminAuditLogService adminAuditLogService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserController(userService),
                        new AdminMerchantApplicationController(merchantApplicationService, adminAuditLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void submit_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/user/merchant-application")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Test Shop\",\"categoryId\":1,\"idCardNo\":\"ID-001\",\"bankCardNo\":\"BANK-001\",\"licenseImg\":\"/license.png\",\"warehouseAddr\":\"No.1 Road\",\"warehouseProvince\":\"Beijing\",\"warehouseCity\":\"Beijing\",\"warehouseDetail\":\"No.1 Road\",\"contactName\":\"Owner\",\"contactPhone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(userService).submitMerchantApplication(any());
    }

    @Test
    void getMyApplication_shouldReturnRecord() throws Exception {
        MerchantApplicationVO application = new MerchantApplicationVO();
        application.setId(8L);
        application.setStoreName("Test Shop");
        application.setStatus(0);
        when(userService.getMyMerchantApplication()).thenReturn(application);

        mockMvc.perform(get("/api/user/merchant-application/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void page_shouldReturnAdminApplicationQueue() throws Exception {
        PageVO<MerchantApplicationVO> page = new PageVO<>();
        page.setTotal(1L);
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setRecords(java.util.List.of(new MerchantApplicationVO()));
        when(merchantApplicationService.pageForAdmin(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/merchant-applications")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNum").value(1));

        verify(merchantApplicationService).pageForAdmin(any());
    }

    @Test
    void reject_shouldRequireReason() throws Exception {
        mockMvc.perform(post("/api/admin/merchant-applications/8/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(merchantApplicationService, never()).reject(any(), any());
    }

    @Test
    void reject_shouldReturnSuccessAndRecordAudit() throws Exception {
        mockMvc.perform(post("/api/admin/merchant-applications/8/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"License information is incomplete\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(merchantApplicationService).reject(eq(8L), any());
        verify(adminAuditLogService).record(eq("REJECT_MERCHANT_APPLICATION"), eq("MERCHANT_APPLICATION"),
                eq(8L), any());
    }

    @Test
    void approve_shouldReturnSuccessAndRecordAudit() throws Exception {
        mockMvc.perform(post("/api/admin/merchant-applications/8/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(merchantApplicationService).approve(8L);
        verify(adminAuditLogService).record("APPROVE_MERCHANT_APPLICATION", "MERCHANT_APPLICATION", 8L,
                "通过入驻申请");
    }
}
