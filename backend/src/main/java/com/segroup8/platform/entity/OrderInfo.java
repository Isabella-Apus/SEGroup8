package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long buyerUserId;
    private BigDecimal totalAmount;
    private Integer payStatus;
    private Integer orderStatus;
    private Integer refundStatus;
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
    private LocalDateTime createTime;
    private LocalDateTime paidTime;
    private LocalDateTime shippedTime;
    private LocalDateTime receivedTime;
    private LocalDateTime completedTime;
    private LocalDateTime closedTime;
    private LocalDateTime refundApplyTime;
    private LocalDateTime refundDecisionTime;
    private Long refundDecisionUserId;
    private String refundDecisionRemark;
    private String refundDecisionSource;
    private Long logisticsTemplateId;
    private String logisticsStatus;
    private Integer logisticsCurrentIndex;
    private Integer canRefund;
    private LocalDateTime afterSalesDeadline;
    private LocalDateTime deliveryTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime autoConfirmDeadline;
    private String refundMode;
    private Long voucherId;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal sellerBearAmount;
    private BigDecimal platformBearAmount;
    private BigDecimal payableAmount;
    @Version
    private Integer version;
    private LocalDateTime updateTime;

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

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
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

    public Long getLogisticsTemplateId() {
        return logisticsTemplateId;
    }

    public void setLogisticsTemplateId(Long logisticsTemplateId) {
        this.logisticsTemplateId = logisticsTemplateId;
    }

    public String getLogisticsStatus() {
        return logisticsStatus;
    }

    public void setLogisticsStatus(String logisticsStatus) {
        this.logisticsStatus = logisticsStatus;
    }

    public Integer getLogisticsCurrentIndex() {
        return logisticsCurrentIndex;
    }

    public void setLogisticsCurrentIndex(Integer logisticsCurrentIndex) {
        this.logisticsCurrentIndex = logisticsCurrentIndex;
    }

    public Integer getCanRefund() {
        return canRefund;
    }

    public void setCanRefund(Integer canRefund) {
        this.canRefund = canRefund;
    }

    public LocalDateTime getAfterSalesDeadline() {
        return afterSalesDeadline;
    }

    public void setAfterSalesDeadline(LocalDateTime afterSalesDeadline) {
        this.afterSalesDeadline = afterSalesDeadline;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalDateTime getAutoConfirmDeadline() {
        return autoConfirmDeadline;
    }

    public void setAutoConfirmDeadline(LocalDateTime autoConfirmDeadline) {
        this.autoConfirmDeadline = autoConfirmDeadline;
    }

    public String getRefundMode() {
        return refundMode;
    }

    public void setRefundMode(String refundMode) {
        this.refundMode = refundMode;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public BigDecimal getVoucherDiscountAmount() {
        return voucherDiscountAmount;
    }

    public void setVoucherDiscountAmount(BigDecimal voucherDiscountAmount) {
        this.voucherDiscountAmount = voucherDiscountAmount;
    }

    public BigDecimal getSellerBearAmount() {
        return sellerBearAmount;
    }

    public void setSellerBearAmount(BigDecimal sellerBearAmount) {
        this.sellerBearAmount = sellerBearAmount;
    }

    public BigDecimal getPlatformBearAmount() {
        return platformBearAmount;
    }

    public void setPlatformBearAmount(BigDecimal platformBearAmount) {
        this.platformBearAmount = platformBearAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
