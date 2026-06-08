package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.PayOrderRequest;
import com.segroup8.platform.dto.OrderItemReviewBatchSubmitRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.dto.OrderReviewSubmitRequest;
import com.segroup8.platform.dto.OrderShipRequest;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @Operation(summary = "分页查询我的订单")
    @GetMapping("/list")
    public Result<PageVO<OrderVO>> list(@Valid @ModelAttribute OrderPageQueryRequest request) {
        return Result.success(orderService.pageMyOrders(request));
    }

    @Operation(summary = "获取我的订单详情")
    @GetMapping("/detail/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.getMyOrderDetail(orderId));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{orderId}/pay")
    public Result<OrderVO> pay(@PathVariable Long orderId, @RequestBody(required = false) PayOrderRequest request) {
        return Result.success(orderService.payMyOrder(orderId, request));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderId}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long orderId) {
        return Result.success(orderService.cancelMyOrder(orderId));
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderId}/confirm-receive")
    public Result<OrderVO> confirmReceive(@PathVariable Long orderId) {
        return Result.success(orderService.confirmReceiveMyOrder(orderId));
    }

    @Operation(summary = "完成订单（评价完成）")
    @PostMapping("/{orderId}/complete")
    public Result<OrderVO> complete(@PathVariable Long orderId) {
        return Result.success(orderService.completeMyOrder(orderId));
    }

    @Operation(summary = "提交订单评价")
    @PostMapping("/{orderId}/review")
    public Result<OrderVO> review(@PathVariable Long orderId, @Valid @RequestBody OrderReviewSubmitRequest request) {
        return Result.success(orderService.submitMyOrderReview(orderId, request));
    }

    @Operation(summary = "按商品逐条提交订单评价")
    @PostMapping("/{orderId}/review/items")
    public Result<OrderVO> reviewItems(@PathVariable Long orderId,
            @Valid @RequestBody OrderItemReviewBatchSubmitRequest request) {
        return Result.success(orderService.submitMyOrderItemReviews(orderId, request));
    }

    @Operation(summary = "申请退货")
    @PostMapping("/{orderId}/refund")
    public Result<OrderVO> refund(@PathVariable Long orderId,
            @RequestBody(required = false) OrderRefundApplyRequest request) {
        return Result.success(orderService.refundMyOrder(orderId, request));
    }

    @Operation(summary = "卖家分页查询订单")
    @GetMapping("/seller/list")
    public Result<PageVO<OrderVO>> sellerList(@Valid @ModelAttribute OrderPageQueryRequest request) {
        return Result.success(orderService.pageSellerOrders(request));
    }

    @Operation(summary = "卖家查看订单详情")
    @GetMapping("/seller/detail/{orderId}")
    public Result<OrderVO> sellerDetail(@PathVariable Long orderId) {
        return Result.success(orderService.getSellerOrderDetail(orderId));
    }

    @Operation(summary = "卖家发货")
    @PostMapping("/{orderId}/ship")
    public Result<OrderVO> ship(@PathVariable Long orderId,
            @Valid @RequestBody(required = false) OrderShipRequest request) {
        return Result.success(orderService.shipSellerOrder(orderId, request));
    }

    @Operation(summary = "卖家同意退货")
    @PostMapping("/{orderId}/refund/approve")
    public Result<OrderVO> approveRefund(@PathVariable Long orderId) {
        return Result.success(orderService.approveRefundBySeller(orderId));
    }

    @Operation(summary = "卖家拒绝退货")
    @PostMapping("/{orderId}/refund/reject")
    public Result<OrderVO> rejectRefund(@PathVariable Long orderId) {
        return Result.success(orderService.rejectRefundBySeller(orderId));
    }

    @Operation(summary = "提醒发货")
    @PostMapping("/{orderId}/remind-ship")
    public Result<Void> remindShip(@PathVariable Long orderId) {
        orderService.remindShipMyOrder(orderId);
        return Result.success();
    }
}
