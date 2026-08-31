package com.segroup8.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record CreateOrderRequest(
            @NotEmpty List<@Valid CreateOrderItem> items,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            @NotBlank String receiverProvince,
            @NotBlank String receiverCity,
            @NotBlank String receiverDetailAddress,
            Long voucherId,
            @Size(max = 255) String remark) {}

    public record CreateOrderItem(@NotNull Long productId, @Min(1) int quantity) {}

    public record SecondhandOrderRequest(
            @NotBlank String tradeType,
            @NotBlank String tradeId,
            @NotBlank String orderBusinessKey,
            @NotNull Long buyerUserId,
            @NotNull Long sellerUserId,
            @NotNull Long productId,
            @NotBlank String productName,
            @NotNull BigDecimal price,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            @NotBlank String receiverProvince,
            @NotBlank String receiverCity,
            @NotBlank String receiverDetailAddress,
            @Size(max = 255) String remark) {}

    public record SecondhandOrderReceipt(long orderId, String orderNo, String status) {}

    public record PayRequest(String payMode, String payChannel) {}
    public record ShipRequest(String deliveryNo, String originProvince, String originCity, String originDetail) {}
    public record RefundRequest(@Size(max = 255) String reason, String refundMode, List<String> proofUrls) {}
    public record RefundDecision(@Size(max = 255) String remark) {}
    public record ReviewRequest(@Min(1) @Max(5) int score, @NotBlank @Size(max = 500) String content) {}
    public record ItemReviewRequest(@NotBlank String productType, @NotNull Long productId,
            @Min(1) @Max(5) int score, @NotBlank @Size(max = 500) String content) {}
    public record ItemReviewsRequest(@NotEmpty List<@Valid ItemReviewRequest> items) {}
    public record ReviewReplyRequest(@Size(max = 500) String reply, @Size(max = 500) String content) {
        String resolvedReply() { return reply == null || reply.isBlank() ? content : reply; }
    }
    public record FollowUpReviewRequest(@NotNull Long orderId, @NotBlank String productType,
            @NotNull Long productId, @Min(1) @Max(5) int score,
            @NotBlank @Size(max = 500) String content) {}
    public record LogisticsPushRequest(@NotNull Long orderId, String nodeName, String statusDesc) {}
    public record BatchCloseRequest(@NotEmpty List<@NotNull Long> orderIds) {}

    public record OrderItemView(long id, String productType, long productId, String productName,
            BigDecimal price, int quantity, Long sellerUserId, Long shopId) {}
    public record OrderView(long id, String orderNo, long buyerUserId, BigDecimal totalAmount,
            BigDecimal payableAmount, String payStatus, OrderState orderStatus, String refundStatus,
            String refundReason, String deliveryNo, String receiverName, String receiverPhone,
            String receiverProvince, String receiverCity, String receiverDetailAddress,
            String receiverPhoneMasked, String receiverRegion, String remark, int version, Instant createTime, Instant updateTime,
            List<OrderItemView> items) {}
    public record PublicOrderView(long id, String orderNo, long buyerUserId, BigDecimal totalAmount,
            BigDecimal payableAmount, int payStatus, String payStatusName, int orderStatus,
            String orderStatusName, String orderStatusKey, int refundStatus, String refundStatusName,
            String refundStatusKey, String refundReason, String deliveryNo, String receiverName,
            String receiverPhone, String receiverProvince, String receiverCity, String receiverDetailAddress,
            String receiverPhoneMasked, String receiverRegion, String remark, String productType,
            int version, Instant createTime, Instant updateTime, List<OrderItemView> items) {}
    public record PageView<T>(long total, long pageNum, long pageSize, List<T> records) {}
    public record ReviewView(long id, long orderId, String productType, long productId, long userId,
            int score, String content, String reviewType, String sellerReply, Instant createTime) {}
    public record LogisticsView(long id, long orderId, String nodeName, String statusDesc, Instant createTime) {}
    public record AfterSaleLogView(long id, long orderId, String action, Long operatorUserId,
            String operatorRole, String remark, Instant createTime) {}
    public record InternalSnapshot(long orderId, String orderNo, long buyerUserId, BigDecimal payableAmount,
            OrderState status, List<OrderItemView> items) {}
    public record ApiResponse<T>(Object code, String message, T data) {
        static <T> ApiResponse<T> success(T data) { return new ApiResponse<>(0, "success", data); }
        static ApiResponse<Void> success() { return success(null); }
    }

    static PublicOrderView publicOrder(OrderView order) {
        int orderStatus = switch (order.orderStatus()) {
            case CREATING, PENDING_PAY, PAYMENT_PENDING -> 0;
            case PENDING_SHIP -> 1;
            case SHIPPED -> 2;
            case RECEIVED -> 3;
            case COMPLETED, REFUNDED -> 4;
            case CANCEL_PENDING, CANCELLED -> 9;
            case REFUND_PENDING -> 1;
        };
        String orderStatusName = switch (orderStatus) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "待评价";
            case 4 -> "已完成";
            default -> "已关闭";
        };
        int refundStatus = switch (order.refundStatus()) {
            case "REQUESTED", "REFUND_PENDING" -> 1;
            case "REFUNDED" -> 2;
            case "REJECTED" -> 3;
            default -> 0;
        };
        String refundStatusName = switch (refundStatus) {
            case 1 -> "售后处理中";
            case 2 -> "退款完成";
            case 3 -> "退款拒绝";
            default -> "无售后";
        };
        boolean paid = "PAID".equals(order.payStatus());
        String productType = order.items().stream().map(OrderItemView::productType).distinct().count() == 1
                && !order.items().isEmpty() ? order.items().get(0).productType() : "MIXED";
        return new PublicOrderView(order.id(), order.orderNo(), order.buyerUserId(), order.totalAmount(),
                order.payableAmount(), paid ? 1 : 0, paid ? "已支付" : "未支付", orderStatus,
                orderStatusName, order.orderStatus().name(), refundStatus, refundStatusName, order.refundStatus(),
                order.refundReason(), order.deliveryNo(), order.receiverName(), order.receiverPhone(),
                order.receiverProvince(), order.receiverCity(), order.receiverDetailAddress(),
                order.receiverPhoneMasked(), order.receiverRegion(), order.remark(), productType, order.version(),
                order.createTime(), order.updateTime(), order.items());
    }

    static PageView<PublicOrderView> publicOrders(PageView<OrderView> page) {
        return new PageView<>(page.total(), page.pageNum(), page.pageSize(),
                page.records().stream().map(ApiModels::publicOrder).toList());
    }

    static SecondhandOrderReceipt secondhandReceipt(OrderView order) {
        return new SecondhandOrderReceipt(order.id(), order.orderNo(), order.orderStatus().name());
    }
}
