package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.AdminUserService;
import com.segroup8.platform.vo.AdminAuditLogVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.UserVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                        new AdminUserController(adminUserService, adminAuditLogService),
                        new AdminAuditLogController(adminAuditLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void pageUsers_shouldReturnAdminUserPage() throws Exception {
        UserVO user = new UserVO();
        user.setId(2L);
        user.setUsername("user");
        PageVO<UserVO> page = new PageVO<>();
        page.setTotal(1L);
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setRecords(List.of(user));
        when(adminUserService.pageUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(2));

        verify(adminUserService).pageUsers(any());
    }

    @Test
    void pageAuditLogs_shouldReturnAdminAuditPage() throws Exception {
        AdminAuditLogVO log = new AdminAuditLogVO();
        log.setId(1L);
        log.setAction("BAN_USER");
        PageVO<AdminAuditLogVO> page = new PageVO<>();
        page.setTotal(1L);
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setRecords(List.of(log));
        when(adminAuditLogService.pageLogs(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].action").value("BAN_USER"));

        verify(adminAuditLogService).pageLogs(any());
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
