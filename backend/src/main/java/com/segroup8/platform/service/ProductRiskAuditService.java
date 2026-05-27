package com.segroup8.platform.service;

import com.segroup8.platform.dto.ProductRiskAuditDecisionRequest;
import com.segroup8.platform.dto.ProductRiskAuditQueryRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductRiskAuditVO;

public interface ProductRiskAuditService {

    void auditNewProduct(Product product);

    void auditSecondhandProduct(SecondhandProduct product);

    ProductRiskAuditVO getLatestAudit(String productType, Long productId);

    PageVO<ProductRiskAuditVO> pageAudits(ProductRiskAuditQueryRequest request);

    ProductRiskAuditVO decide(Long auditId, ProductRiskAuditDecisionRequest request);
}
