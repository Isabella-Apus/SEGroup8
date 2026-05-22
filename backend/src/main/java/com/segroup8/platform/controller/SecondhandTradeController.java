package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/secondhand/trade")
public class SecondhandTradeController {

    private final SecondhandTradeService secondhandTradeService;

    public SecondhandTradeController(SecondhandTradeService secondhandTradeService) {
        this.secondhandTradeService = secondhandTradeService;
    }

    @Operation(summary = "买家发起二手议价")
    @PostMapping("/bargain/apply")
    public Result<Map<String, Object>> applyBargain(@RequestBody(required = false) Map<String, Object> request) {
        return Result.success(secondhandTradeService.applyBargain(emptyIfNull(request)));
    }

    @Operation(summary = "卖家同意二手议价")
    @PostMapping("/bargain/confirm")
    public Result<Map<String, Object>> confirmBargain(@RequestBody(required = false) Map<String, Object> request) {
        return Result.success(secondhandTradeService.confirmBargain(emptyIfNull(request)));
    }

    @Operation(summary = "卖家拒绝二手议价")
    @PostMapping("/bargain/{negotiationId}/reject")
    public Result<Map<String, Object>> rejectBargain(@PathVariable Long negotiationId) {
        return Result.success(secondhandTradeService.rejectBargain(negotiationId));
    }

    @Operation(summary = "查询二手议价记录")
    @GetMapping("/bargain/list")
    public Result<PageVO<Map<String, Object>>> pageBargains(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long counterpartUserId,
            @RequestParam(required = false) String status) {
        return Result.success(secondhandTradeService.pageBargains(pageNum, pageSize, productId, counterpartUserId, status));
    }

    @Operation(summary = "查询当前买家可用议价")
    @GetMapping("/bargain/effective")
    public Result<Map<String, Object>> getMyEffectiveBargain(@RequestParam Long productId) {
        return Result.success(secondhandTradeService.getMyEffectiveBargain(productId));
    }

    @Operation(summary = "卖家发起二手拍卖")
    @PostMapping("/auction")
    public Result<Map<String, Object>> createAuction(@RequestBody(required = false) Map<String, Object> request) {
        return Result.success(secondhandTradeService.createAuction(emptyIfNull(request)));
    }

    @Operation(summary = "按商品查询二手拍卖")
    @GetMapping("/auction/product/{productId}")
    public Result<Map<String, Object>> getAuctionByProductId(@PathVariable Long productId) {
        return Result.success(secondhandTradeService.getAuctionByProductId(productId));
    }

    @Operation(summary = "卖家查询自己的二手拍卖")
    @GetMapping("/auction/seller/list")
    public Result<PageVO<Map<String, Object>>> pageMyAuctions(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(secondhandTradeService.pageMyAuctions(pageNum, pageSize, status));
    }

    @Operation(summary = "卖家提前结束二手拍卖")
    @PostMapping("/auction/{auctionId}/close")
    public Result<Map<String, Object>> closeAuctionEarly(@PathVariable Long auctionId) {
        return Result.success(secondhandTradeService.closeAuctionEarly(auctionId));
    }

    @Operation(summary = "卖家标记二手拍卖流拍")
    @PostMapping("/auction/{auctionId}/flow")
    public Result<Map<String, Object>> markAuctionFlow(@PathVariable Long auctionId) {
        return Result.success(secondhandTradeService.markAuctionFlow(auctionId));
    }

    @Operation(summary = "买家参与二手拍卖出价")
    @PostMapping("/auction/{auctionId}/bid")
    public Result<Map<String, Object>> placeBid(
            @PathVariable Long auctionId,
            @RequestBody(required = false) Map<String, Object> request) {
        return Result.success(secondhandTradeService.placeBid(auctionId, emptyIfNull(request)));
    }

    private Map<String, Object> emptyIfNull(Map<String, Object> request) {
        return request == null ? Collections.emptyMap() : request;
    }
}
