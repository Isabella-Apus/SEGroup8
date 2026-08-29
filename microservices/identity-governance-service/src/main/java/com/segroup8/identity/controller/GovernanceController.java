package com.segroup8.identity.controller;

import com.segroup8.identity.api.ApiResult;
import com.segroup8.identity.security.CurrentUser;
import com.segroup8.identity.service.IdentityService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class GovernanceController {
    private final IdentityService service;

    public GovernanceController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/api/report-block/report")
    public ApiResult<Void> report(@RequestBody Map<String, Object> request) {
        service.submitReport(request);
        return ApiResult.success();
    }

    @GetMapping("/api/report-block/report/my")
    public ApiResult<Map<String, Object>> myReports(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(service.myReports(page, size));
    }

    @PostMapping("/api/report-block/block")
    public ApiResult<Void> block(@RequestBody Map<String, Object> request) {
        service.block(((Number) request.get("targetUserId")).longValue());
        return ApiResult.success();
    }

    @DeleteMapping("/api/report-block/block/{id}")
    public ApiResult<Void> unblock(@PathVariable long id) {
        service.unblock(id);
        return ApiResult.success();
    }

    @GetMapping("/api/report-block/block/my")
    public ApiResult<List<Map<String, Object>>> blocks() {
        return ApiResult.success(service.myBlocks());
    }

    @GetMapping("/api/report-block/block/check/{id}")
    public ApiResult<Boolean> isBlocking(@PathVariable long id) {
        return ApiResult.success(service.isBlocking(CurrentUser.require().userId(), id));
    }

    @GetMapping("/api/report-block/block/blocked-by/{id}")
    public ApiResult<Boolean> isBlockedBy(@PathVariable long id) {
        return ApiResult.success(service.isBlocking(id, CurrentUser.require().userId()));
    }

    @GetMapping("/api/credit/me")
    public ApiResult<Map<String, Object>> myCredit() {
        return ApiResult.success(service.credit(CurrentUser.require().userId()));
    }

    @GetMapping("/api/credit/{id}")
    public ApiResult<Map<String, Object>> credit(@PathVariable long id) {
        return ApiResult.success(service.credit(id));
    }

    @GetMapping("/api/admin/reports")
    public ApiResult<Map<String, Object>> reports(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long reportedId) {
        return ApiResult.success(service.reports(status, reportedId, page, size));
    }

    @PostMapping("/api/admin/reports/audit")
    public ApiResult<Void> audit(@RequestBody Map<String, Object> request) {
        Number delta = (Number) request.get("customDelta");
        service.auditReport(((Number) request.get("reportId")).longValue(),
                ((Number) request.get("decision")).intValue(), (String) request.get("adminRemark"),
                delta == null ? null : delta.intValue());
        return ApiResult.success();
    }

    @PostMapping("/api/admin/reports/credit-adjust")
    public ApiResult<Void> adjust(@RequestParam long userId, @RequestParam String role,
            @RequestParam int delta, @RequestParam(defaultValue = "管理员手动调整") String remark) {
        service.adjustCredit(userId, role, delta, remark);
        return ApiResult.success();
    }
}
