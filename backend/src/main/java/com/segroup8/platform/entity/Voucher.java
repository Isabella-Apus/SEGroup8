package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("voucher")
public class Voucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 1=SELLER, 2=ADMIN
     */
    private Integer issuerType;

    /**
     * 1=SELLER_VOUCHER, 2=PLATFORM_VOUCHER
     */
    private Integer voucherType;

    private Long issuerUserId;

    /**
     * 1=SHOP, 2=PLATFORM, 3=PRODUCT
     */
    private Integer scopeType;

    private Long shopId;

    private Long productId;

    /**
     * false=不可叠加, true=可叠加
     */
    private Boolean canStack;

    private String name;

    private Integer type;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer totalCount;

    private Integer receivedCount;

    private Integer usedCount;

    /**
     * 抢券开始时间
     */
    private LocalDateTime grabStartTime;

    /**
     * 抢券结束时间
     */
    private LocalDateTime grabEndTime;

    /**
     * 使用有效期开始时间
     */
    private LocalDateTime startTime;

    /**
     * 使用有效期结束时间
     */
    private LocalDateTime endTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}