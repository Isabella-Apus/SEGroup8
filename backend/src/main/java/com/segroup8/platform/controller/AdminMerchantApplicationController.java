package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AdminMerchantApplicationQueryRequest;
import com.segroup8.platform.dto.MerchantApplicationRejectRequest;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/merchant-applications")
public class AdminMerchantApplicationController {

    private final MerchantApplicationService merchantApplicationService;
    private final AdminAuditLogService adminAuditLogService;

    public AdminMerchantApplicationController(MerchantApplicationService merchantApplicationService,
            AdminAuditLogService adminAuditLogService) {
        this.merchantApplicationService = merchantApplicationService;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Operation(summary = "管理员分页查看入驻申请")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"total\":1,\"pageNum\":1,\"pageSize\":10,\"records\":[{\"id\":8,\"storeName\":\"好物小店\",\"status\":0}]}}")))
    @GetMapping
    public Result<PageVO<MerchantApplicationVO>> page(AdminMerchantApplicationQueryRequest request) {
        return Result.success(merchantApplicationService.pageForAdmin(request));
    }

    @Operation(summary = "通过入驻申请")
    @ApiResponse(responseCode = "200", description = "审核通过成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PostMapping("/{applicationId}/approve")
    public Result<Void> approve(@PathVariable Long applicationId) {
        merchantApplicationService.approve(applicationId);
        adminAuditLogService.record("APPROVE_MERCHANT_APPLICATION", "MERCHANT_APPLICATION", applicationId, "通过入驻申请");
        return Result.success();
    }

    @Operation(summary = "驳回入驻申请")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MerchantApplicationRejectRequest.class), examples = @ExampleObject(value = "{\n  \"rejectReason\": \"营业执照信息不完整\"\n}")))
    @ApiResponse(responseCode = "200", description = "驳回成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PostMapping("/{applicationId}/reject")
    public Result<Void> reject(@PathVariable Long applicationId,
            @Valid @RequestBody MerchantApplicationRejectRequest request) {
        merchantApplicationService.reject(applicationId, request);
        adminAuditLogService.record("REJECT_MERCHANT_APPLICATION", "MERCHANT_APPLICATION", applicationId,
                "驳回入驻申请: " + request.getRejectReason());
        return Result.success();
    }
}
