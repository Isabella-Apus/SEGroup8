package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
}
