package com.segroup8.platform.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductAuctionVO {

    private Long id;
    private Long productId;
    private String productName;
    private Long sellerUserId;
    private BigDecimal startPrice;
    private BigDecimal incrementAmount;
    private BigDecimal currentPrice;
    private Long currentBidderUserId;
    private String currentBidderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String statusName;
    private Long settledOrderId;
    private Long bidCount;
    private List<AuctionLogVO> logs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getIncrementAmount() {
        return incrementAmount;
    }

    public void setIncrementAmount(BigDecimal incrementAmount) {
        this.incrementAmount = incrementAmount;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Long getCurrentBidderUserId() {
        return currentBidderUserId;
    }

    public void setCurrentBidderUserId(Long currentBidderUserId) {
        this.currentBidderUserId = currentBidderUserId;
    }

    public String getCurrentBidderName() {
        return currentBidderName;
    }

    public void setCurrentBidderName(String currentBidderName) {
        this.currentBidderName = currentBidderName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Long getSettledOrderId() {
        return settledOrderId;
    }

    public void setSettledOrderId(Long settledOrderId) {
        this.settledOrderId = settledOrderId;
    }

    public Long getBidCount() {
        return bidCount;
    }

    public void setBidCount(Long bidCount) {
        this.bidCount = bidCount;
    }

    public List<AuctionLogVO> getLogs() {
        return logs;
    }

    public void setLogs(List<AuctionLogVO> logs) {
        this.logs = logs;
    }
}
