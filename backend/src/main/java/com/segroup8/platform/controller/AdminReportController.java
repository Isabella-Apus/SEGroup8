package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AdminReportAuditRequest;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.entity.UserReport;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.service.ReportBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员-举报审核", description = "管理员查看并审核用户举报")
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportBlockService reportBlockService;
    private final AdminAuditLogService adminAuditLogService;
    private final CreditService creditService;
    private final UserMapper userMapper;

    public AdminReportController(ReportBlockService reportBlockService,
                                  AdminAuditLogService adminAuditLogService,
                                  CreditService creditService,
                                  UserMapper userMapper) {
        this.reportBlockService = reportBlockService;
        this.adminAuditLogService = adminAuditLogService;
        this.creditService = creditService;
        this.userMapper = userMapper;
    }

    /**
     * 管理员分页查询举报列表
     * GET /api/admin/reports?page=1&size=10&status=0&reportedId=
     *
     * status: 0=待审核 1=成立 2=驳回 不传=全部
     * reportedId: 筛选特定被举报人，不传=全部
     */
    @Operation(summary = "管理员分页查询举报列表")
    @GetMapping
    public Result<IPage<UserReport>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long reportedId) {
        AccessControl.requireAdmin(userMapper);
        return Result.success(reportBlockService.adminListReports(page, size, status, reportedId));
    }

    /**
     * 管理员审核举报（成立扣分 or 驳回）
     * POST /api/admin/reports/audit
     * Body:
     * {
     *   "reportId": 1,
     *   "decision": 1,        // 1=成立扣分  2=驳回
     *   "adminRemark": "核实属实",
     *   "customDelta": 10     // 可选，不填则按举报类型自动计算扣分
     * }
     */
    @Operation(summary = "管理员审核举报")
    @PostMapping("/audit")
    public Result<Void> auditReport(@Valid @RequestBody AdminReportAuditRequest request) {
        AccessControl.requireAdmin(userMapper);
        reportBlockService.adminAuditReport(request);

        // 写管理员操作日志（复用项目已有的 AdminAuditLogService）
        String action = request.getDecision() == 1 ? "REPORT_UPHELD" : "REPORT_REJECTED";
        adminAuditLogService.record(action, "USER_REPORT", request.getReportId(),
                request.getDecision() == 1 ? "举报成立，已扣分" : "举报不成立，已驳回");

        return Result.success();
    }

    /**
     * 管理员手动调整用户信用分
     * POST /api/admin/reports/credit-adjust
     * Body:
     * {
     *   "userId": 3,
     *   "role": "BUYER",   // BUYER 或 SELLER
     *   "delta": -5,       // 正数加分，负数扣分
     *   "remark": "手动修正"
     * }
     */
    @Operation(summary = "管理员手动调整用户信用分")
    @PostMapping("/credit-adjust")
    public Result<Void> creditAdjust(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam int delta,
            @RequestParam(defaultValue = "管理员手动调整") String remark) {
        AccessControl.requireAdmin(userMapper);
        Long adminId = AccessControl.requireUserId();
        creditService.adminAdjust(userId, role, delta, remark, adminId);
        adminAuditLogService.record("CREDIT_ADJUST", "USER", userId,
                "管理员调整" + role + "信用分：" + delta);
        return Result.success();
    }
}