package com.segroup8.identity.controller;

import com.segroup8.identity.api.ApiResult;
import com.segroup8.identity.service.IdentityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final IdentityService service;

    public AdminController(IdentityService service) {
        this.service = service;
    }

    @GetMapping("/merchant-applications")
    public ApiResult<Map<String, Object>> merchantApplications(@RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.success(service.merchantApplications(status, pageNum, pageSize));
    }

    @PostMapping("/merchant-applications/{id}/approve")
    public ApiResult<Void> approve(@PathVariable long id) {
        service.approveMerchant(id);
        return ApiResult.success();
    }

    @PostMapping("/merchant-applications/{id}/reject")
    public ApiResult<Void> reject(@PathVariable long id, @RequestBody Map<String, Object> request) {
        service.rejectMerchant(id, String.valueOf(request.getOrDefault("rejectReason", "")));
        return ApiResult.success();
    }

    @GetMapping("/users")
    public ApiResult<Map<String, Object>> users(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.success(service.users(pageNum, pageSize));
    }

    @PutMapping("/users/{id}/ban")
    public ApiResult<Void> ban(@PathVariable long id) {
        service.changeBan(id, true);
        return ApiResult.success();
    }

    @PutMapping("/users/{id}/unban")
    public ApiResult<Void> unban(@PathVariable long id) {
        service.changeBan(id, false);
        return ApiResult.success();
    }

    @GetMapping("/audit-logs")
    public ApiResult<Map<String, Object>> auditLogs(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String targetType) {
        return ApiResult.success(service.auditLogs(pageNum, pageSize, targetType));
    }
}
