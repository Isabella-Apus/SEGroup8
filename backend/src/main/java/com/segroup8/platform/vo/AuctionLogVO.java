package com.segroup8.platform.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionLogVO {

    private Long id;
    private Long bidderUserId;
    private String bidderName;
    private BigDecimal bidAmount;
    private String status;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBidderUserId() {
        return bidderUserId;
    }

    public void setBidderUserId(Long bidderUserId) {
        this.bidderUserId = bidderUserId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
