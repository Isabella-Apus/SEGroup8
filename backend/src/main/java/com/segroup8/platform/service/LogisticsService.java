package com.segroup8.platform.service;

import com.segroup8.platform.dto.OrderShipRequest;
import com.segroup8.platform.vo.LogisticsTraceVO;

import java.util.List;

public interface LogisticsService {

    LogisticsTraceVO pushNextBySeller(Long orderId);

    List<LogisticsTraceVO> listByOrderId(Long orderId);

    void initializeWhenShipped(Long orderId, OrderShipRequest request);
}
