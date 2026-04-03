package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AdminUserQueryRequest;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.AdminUserService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminAuditLogService adminAuditLogService;

    public AdminUserController(AdminUserService adminUserService,
            AdminAuditLogService adminAuditLogService) {
        this.adminUserService = adminUserService;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Operation(summary = "管理员分页查询用户")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"total\":1,\"pageNum\":1,\"pageSize\":10,\"records\":[{\"id\":3,\"username\":\"user\",\"status\":\"NORMAL\"}]}}")))
    @GetMapping
    public Result<PageVO<UserVO>> pageUsers(@Valid AdminUserQueryRequest request) {
        return Result.success(adminUserService.pageUsers(request));
    }

    @Operation(summary = "封禁用户")
    @ApiResponse(responseCode = "200", description = "封禁成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PutMapping("/{userId}/ban")
    public Result<Void> banUser(@PathVariable Long userId) {
        adminUserService.banUser(userId);
        adminAuditLogService.record("BAN_USER", "USER", userId, "管理员封禁用户");
        return Result.success();
    }

    @Operation(summary = "解封用户")
    @ApiResponse(responseCode = "200", description = "解封成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PutMapping("/{userId}/unban")
    public Result<Void> unbanUser(@PathVariable Long userId) {
        adminUserService.unbanUser(userId);
        adminAuditLogService.record("UNBAN_USER", "USER", userId, "管理员解封用户");
        return Result.success();
    }
}
