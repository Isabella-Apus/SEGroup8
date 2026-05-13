package com.segroup8.platform.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderAutoConfirmScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoConfirmScheduler.class);

    private final OrderInfoMapper orderInfoMapper;
    private final OrderService orderService;

    public OrderAutoConfirmScheduler(OrderInfoMapper orderInfoMapper, OrderService orderService) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderService = orderService;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void autoConfirmDeliveredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderStatus, OrderStatusEnum.SHIPPED.getCode())
                .eq(OrderInfo::getLogisticsStatus, "ARRIVED")
                .isNotNull(OrderInfo::getAutoConfirmDeadline)
                .le(OrderInfo::getAutoConfirmDeadline, now)
                .last("limit 200"));
        for (OrderInfo order : orders) {
            try {
                orderService.autoConfirmReceiveForSystem(order.getId());
            } catch (Exception ex) {
                log.warn("auto confirm order failed, orderId={}", order.getId(), ex);
            }
        }
    }

    @Scheduled(cron = "0 15 * * * ?")
    public void autoApproveTimeoutRefundOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .le("refund_apply_time", deadline)
                .last("limit 200");
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        for (OrderInfo order : orders) {
            try {
                orderService.autoApproveRefundForSystem(order.getId());
            } catch (Exception ex) {
                log.warn("auto approve refund failed, orderId={}", order.getId(), ex);
            }
        }
    }
}
