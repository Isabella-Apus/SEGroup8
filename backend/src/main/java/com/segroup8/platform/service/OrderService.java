package com.segroup8.platform.service;

import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.OrderItemReviewBatchSubmitRequest;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.dto.OrderReviewSubmitRequest;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;

public interface OrderService {

    OrderVO createOrder(CreateOrderRequest request);

    PageVO<OrderVO> pageMyOrders(OrderPageQueryRequest request);

    OrderVO getMyOrderDetail(Long orderId);

    OrderVO payMyOrder(Long orderId);

    OrderVO cancelMyOrder(Long orderId);

    OrderVO confirmReceiveMyOrder(Long orderId);

    OrderVO completeMyOrder(Long orderId);

    OrderVO submitMyOrderReview(Long orderId, OrderReviewSubmitRequest request);

    OrderVO submitMyOrderItemReviews(Long orderId, OrderItemReviewBatchSubmitRequest request);

    OrderVO refundMyOrder(Long orderId, OrderRefundApplyRequest request);

    OrderVO approveRefundBySeller(Long orderId);

    OrderVO rejectRefundBySeller(Long orderId);

    PageVO<OrderVO> pageSellerOrders(OrderPageQueryRequest request);

    OrderVO getSellerOrderDetail(Long orderId);

    OrderVO shipSellerOrder(Long orderId);

    void remindShipMyOrder(Long orderId);
}
