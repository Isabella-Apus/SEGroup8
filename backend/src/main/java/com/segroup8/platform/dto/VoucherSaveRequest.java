package com.segroup8.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherSaveRequest {

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotNull(message = "类型不能为空")
    private Integer type;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    @NotNull(message = "最低消费金额不能为空")
    @DecimalMin(value = "0", message = "最低消费金额不能为负数")
    private BigDecimal minAmount;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为1")
    private Integer totalCount;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}