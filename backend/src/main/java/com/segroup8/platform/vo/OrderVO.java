package com.segroup8.platform.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderVO {

    private Long id;
    private String orderNo;
    private Long buyerUserId;
    private BigDecimal totalAmount;
    private Integer payStatus;
    private Integer orderStatus;
    private String orderStatusName;
    private Integer refundStatus;
    private String refundStatusName;
    private String refundReason;
    private String refundProofUrls;
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDetailAddress;
    private String payMethod;
    private String deliveryNo;
    private String remark;
    private String tradeMode;
    private LocalDateTime createTime;
    private LocalDateTime paidTime;
    private LocalDateTime shippedTime;
    private LocalDateTime receivedTime;
    private LocalDateTime completedTime;
    private LocalDateTime closedTime;
    private LocalDateTime refundApplyTime;
    private LocalDateTime refundDecisionTime;
    private Long refundDecisionUserId;
    private String refundDecisionUserName;
    private String refundDecisionRemark;
    private String refundDecisionSource;
    private List<OrderItemVO> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(Long buyerUserId) {
        this.buyerUserId = buyerUserId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(Integer payStatus) {
        this.payStatus = payStatus;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatusName() {
        return orderStatusName;
    }

    public void setOrderStatusName(String orderStatusName) {
        this.orderStatusName = orderStatusName;
    }

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getRefundStatusName() {
        return refundStatusName;
    }

    public void setRefundStatusName(String refundStatusName) {
        this.refundStatusName = refundStatusName;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundProofUrls() {
        return refundProofUrls;
    }

    public void setRefundProofUrls(String refundProofUrls) {
        this.refundProofUrls = refundProofUrls;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverProvince() {
        return receiverProvince;
    }

    public void setReceiverProvince(String receiverProvince) {
        this.receiverProvince = receiverProvince;
    }

    public String getReceiverCity() {
        return receiverCity;
    }

    public void setReceiverCity(String receiverCity) {
        this.receiverCity = receiverCity;
    }

    public String getReceiverDetailAddress() {
        return receiverDetailAddress;
    }

    public void setReceiverDetailAddress(String receiverDetailAddress) {
        this.receiverDetailAddress = receiverDetailAddress;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getDeliveryNo() {
        return deliveryNo;
    }

    public void setDeliveryNo(String deliveryNo) {
        this.deliveryNo = deliveryNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTradeMode() {
        return tradeMode;
    }

    public void setTradeMode(String tradeMode) {
        this.tradeMode = tradeMode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(LocalDateTime paidTime) {
        this.paidTime = paidTime;
    }

    public LocalDateTime getShippedTime() {
        return shippedTime;
    }

    public void setShippedTime(LocalDateTime shippedTime) {
        this.shippedTime = shippedTime;
    }

    public LocalDateTime getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(LocalDateTime receivedTime) {
        this.receivedTime = receivedTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public LocalDateTime getClosedTime() {
        return closedTime;
    }

    public void setClosedTime(LocalDateTime closedTime) {
        this.closedTime = closedTime;
    }

    public LocalDateTime getRefundApplyTime() {
        return refundApplyTime;
    }

    public void setRefundApplyTime(LocalDateTime refundApplyTime) {
        this.refundApplyTime = refundApplyTime;
    }

    public LocalDateTime getRefundDecisionTime() {
        return refundDecisionTime;
    }

    public void setRefundDecisionTime(LocalDateTime refundDecisionTime) {
        this.refundDecisionTime = refundDecisionTime;
    }

    public Long getRefundDecisionUserId() {
        return refundDecisionUserId;
    }

    public void setRefundDecisionUserId(Long refundDecisionUserId) {
        this.refundDecisionUserId = refundDecisionUserId;
    }

    public String getRefundDecisionUserName() {
        return refundDecisionUserName;
    }

    public void setRefundDecisionUserName(String refundDecisionUserName) {
        this.refundDecisionUserName = refundDecisionUserName;
    }

    public String getRefundDecisionRemark() {
        return refundDecisionRemark;
    }

    public void setRefundDecisionRemark(String refundDecisionRemark) {
        this.refundDecisionRemark = refundDecisionRemark;
    }

    public String getRefundDecisionSource() {
        return refundDecisionSource;
    }

    public void setRefundDecisionSource(String refundDecisionSource) {
        this.refundDecisionSource = refundDecisionSource;
    }

    public List<OrderItemVO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVO> items) {
        this.items = items;
    }
}
