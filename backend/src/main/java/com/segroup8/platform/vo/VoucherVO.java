package com.segroup8.platform.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherVO {

    private Long id;

    private String name;

    private Integer type;

    private String typeName;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer totalCount;

    private Integer usedCount;

    private Integer remainCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusName;

    private LocalDateTime createTime;
}