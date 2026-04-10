package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.LogisticsPushNextRequest;
import com.segroup8.platform.service.LogisticsService;
import com.segroup8.platform.vo.LogisticsTraceVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Operation(summary = "卖家推进物流节点")
    @PostMapping("/push-next")
    public Result<LogisticsTraceVO> pushNext(@Valid @RequestBody LogisticsPushNextRequest request) {
        return Result.success(logisticsService.pushNextBySeller(request.getOrderId()));
    }

    @Operation(summary = "查询订单物流轨迹")
    @GetMapping("/order/{orderId}/trace")
    public Result<List<LogisticsTraceVO>> listByOrderId(@PathVariable Long orderId) {
        return Result.success(logisticsService.listByOrderId(orderId));
    }
}
