package com.segroup8.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherVO {

    private Long id;

    private Long issuerUserId;

    private Integer issuerType;

    private String issuerTypeName;

    /**
     * 1=卖家券, 2=平台券
     */
    private Integer voucherType;

    private String voucherTypeName;

    /**
     * 1=店铺, 2=全平台, 3=商品
     */
    private Integer scopeType;

    private String scopeTypeName;

    private Long shopId;

    private Long productId;

    private Boolean canStack;

    private String name;

    private Integer type;

    private String typeName;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer totalCount;

    private Integer receivedCount;

    private Integer usedCount;

    private Integer remainCount;

    /**
     * 抢券开始时间
     */
    private LocalDateTime grabStartTime;

    /**
     * 抢券结束时间
     */
    private LocalDateTime grabEndTime;

    /**
     * 使用开始时间
     */
    private LocalDateTime startTime;

    /**
     * 使用结束时间
     */
    private LocalDateTime endTime;

    /**
     * 距离到期天数（仅我的优惠券场景返回）
     */
    private Long daysToExpire;

    private Integer status;

    private String statusName;

    /**
     * 我的券状态：1=未使用,2=已使用,3=已过期
     */
    private Integer myStatus;

    private String myStatusName;

    private LocalDateTime createTime;
}
