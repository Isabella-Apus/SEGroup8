package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.ProductRiskAuditDecisionRequest;
import com.segroup8.platform.dto.ProductRiskAuditQueryRequest;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductRiskAuditVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/product-risk-audits")
public class AdminProductRiskAuditController {

    private final ProductRiskAuditService productRiskAuditService;

    public AdminProductRiskAuditController(ProductRiskAuditService productRiskAuditService) {
        this.productRiskAuditService = productRiskAuditService;
    }

    @GetMapping
    public Result<PageVO<ProductRiskAuditVO>> page(@Valid @ModelAttribute ProductRiskAuditQueryRequest request) {
        return Result.success(productRiskAuditService.pageAudits(request));
    }

    @PostMapping("/{auditId}/decision")
    public Result<ProductRiskAuditVO> decide(@PathVariable Long auditId,
            @Valid @RequestBody ProductRiskAuditDecisionRequest request) {
        return Result.success(productRiskAuditService.decide(auditId, request));
    }
}
