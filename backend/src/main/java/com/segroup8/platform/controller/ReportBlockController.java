package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.UserBlockRequest;
import com.segroup8.platform.dto.UserReportRequest;
import com.segroup8.platform.entity.UserBlock;
import com.segroup8.platform.entity.UserReport;
import com.segroup8.platform.service.ReportBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "举报与拉黑", description = "买家/卖家互相举报、拉黑操作")
@RestController
@RequestMapping("/api/report-block")
public class ReportBlockController {

    private final ReportBlockService reportBlockService;

    public ReportBlockController(ReportBlockService reportBlockService) {
        this.reportBlockService = reportBlockService;
    }

    // ==================== 举报 ====================

    /**
     * 提交举报
     * POST /api/report-block/report
     * Body: { "reportedId": 2, "reasonType": "FRAUD", "reasonDesc": "...", "evidenceUrls": "..." }
     */
    @Operation(summary = "提交举报")
    @PostMapping("/report")
    public Result<Void> submitReport(@Valid @RequestBody UserReportRequest request) {
        reportBlockService.submitReport(request);
        return Result.success();
    }

    /**
     * 查询我提交的举报列表
     * GET /api/report-block/report/my?page=1&size=10
     */
    @Operation(summary = "查询我提交的举报列表")
    @GetMapping("/report/my")
    public Result<IPage<UserReport>> myReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(reportBlockService.myReports(page, size));
    }

    // ==================== 拉黑 ====================

    /**
     * 拉黑某用户
     * POST /api/report-block/block
     * Body: { "targetUserId": 5 }
     */
    @Operation(summary = "拉黑某用户")
    @PostMapping("/block")
    public Result<Void> blockUser(@Valid @RequestBody UserBlockRequest request) {
        reportBlockService.blockUser(request);
        return Result.success();
    }

    /**
     * 取消拉黑
     * DELETE /api/report-block/block/{targetUserId}
     */
    @Operation(summary = "取消拉黑")
    @DeleteMapping("/block/{targetUserId}")
    public Result<Void> unblockUser(@PathVariable Long targetUserId) {
        UserBlockRequest request = new UserBlockRequest();
        request.setTargetUserId(targetUserId);
        reportBlockService.unblockUser(request);
        return Result.success();
    }

    /**
     * 查询我的拉黑列表
     * GET /api/report-block/block/my
     */
    @Operation(summary = "查询我的拉黑列表")
    @GetMapping("/block/my")
    public Result<List<UserBlock>> myBlockList() {
        return Result.success(reportBlockService.myBlockList());
    }

    /**
     * 查询我是否拉黑了某用户
     * GET /api/report-block/block/check/{targetUserId}
     */
    @Operation(summary = "查询是否已拉黑某用户")
    @GetMapping("/block/check/{targetUserId}")
    public Result<Boolean> isBlocking(@PathVariable Long targetUserId) {
        return Result.success(reportBlockService.isBlocking(targetUserId));
    }

    /**
     * 查询某用户是否拉黑了我
     * GET /api/report-block/block/blocked-by/{targetUserId}
     */
    @Operation(summary = "查询某用户是否拉黑了我")
    @GetMapping("/block/blocked-by/{targetUserId}")
    public Result<Boolean> isBlockedBy(@PathVariable Long targetUserId) {
        return Result.success(reportBlockService.isBlockedBy(targetUserId));
    }
}