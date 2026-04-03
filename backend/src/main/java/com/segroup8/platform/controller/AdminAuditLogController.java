package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AdminAuditLogQueryRequest;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.vo.AdminAuditLogVO;
import com.segroup8.platform.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @Operation(summary = "管理员分页查询操作审计日志")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"total\":1,\"pageNum\":1,\"pageSize\":10,\"records\":[{\"id\":1,\"adminUsername\":\"admin\",\"action\":\"BAN_USER\",\"targetType\":\"USER\",\"targetId\":3,\"detail\":\"管理员封禁用户\"}]}}")))
    @GetMapping
    public Result<PageVO<AdminAuditLogVO>> page(AdminAuditLogQueryRequest request) {
        return Result.success(adminAuditLogService.pageLogs(request));
    }
}
