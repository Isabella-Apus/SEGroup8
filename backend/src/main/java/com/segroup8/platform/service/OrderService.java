package com.segroup8.platform.service;

import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;

public interface OrderService {

    OrderVO createOrder(CreateOrderRequest request);

    PageVO<OrderVO> pageMyOrders(OrderPageQueryRequest request);
}
