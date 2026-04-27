package com.segroup8.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherSaveRequest {

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    /**
     * 面额类型: 1=满减, 2=折扣
     */
    @NotNull(message = "面额类型不能为空")
    private Integer type;

    /**
     * 优惠券类型: 1=卖家券, 2=平台券
     */
    private Integer voucherType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    @DecimalMin(value = "0", message = "最低消费金额不能为负数")
    private BigDecimal minAmount;

    /**
     * 是否无门槛：true=无门槛，false=有门槛
     */
    private Boolean noThreshold;

    /**
     * 适用范围: 1=店铺, 2=全平台, 3=商品
     */
    private Integer scopeType;

    private Long shopId;

    private Long productId;

    /**
     * 是否可叠加（默认不可叠加）
     */
    private Boolean canStack;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为1")
    private Integer totalCount;

    @NotNull(message = "抢券开始时间不能为空")
    private LocalDateTime grabStartTime;

    @NotNull(message = "抢券结束时间不能为空")
    private LocalDateTime grabEndTime;

    @NotNull(message = "使用开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "使用结束时间不能为空")
    private LocalDateTime endTime;
}
