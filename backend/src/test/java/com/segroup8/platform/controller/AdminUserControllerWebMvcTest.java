package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private AdminAuditLogService adminAuditLogService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminUserController(adminUserService, adminAuditLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void ban_shouldReturnSuccessAndRecordAudit() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(adminUserService).banUser(2L);
        verify(adminAuditLogService).record(eq("BAN_USER"), eq("USER"), eq(2L), anyString());
    }

    @Test
    void unban_shouldReturnSuccessAndRecordAudit() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/unban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(adminUserService).unbanUser(2L);
        verify(adminAuditLogService).record(eq("UNBAN_USER"), eq("USER"), eq(2L), anyString());
    }
}
