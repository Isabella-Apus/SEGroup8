package com.segroup8.secondhand.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SecondhandProduct(long id, long sellerUserId, String sellerNameSnapshot, String name,
        String cover, String imagesJson, String description, BigDecimal originPrice, BigDecimal salePrice,
        int categoryId, int subCategoryId, String conditionLevel, int negotiable, int status,
        String riskStatus, int version, boolean deleted, LocalDateTime createTime, LocalDateTime updateTime) {

    public static final int ON_SHELF = 1;
    public static final int OFF_SHELF = 2;
    public static final int SOLD = 3;
    public static final int TRADE_PENDING = 4;

    public boolean ownedBy(long userId) {
        return sellerUserId == userId;
    }

    public boolean publiclyTradable() {
        return status == ON_SHELF && "APPROVED".equals(riskStatus) && !deleted;
    }
}
