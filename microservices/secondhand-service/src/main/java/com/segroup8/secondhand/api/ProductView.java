package com.segroup8.secondhand.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductView(long id, long sellerUserId, String sellerName, String name, String cover,
        List<String> images, String description, BigDecimal originPrice, BigDecimal salePrice,
        int categoryId, int subCategoryId, String categoryName, String subCategoryName,
        String conditionLevel, int isNegotiable, int status, String statusName,
        RiskView riskAudit, LocalDateTime createTime) {

    public record RiskView(String auditStatus, String suggestion) {
    }
}
