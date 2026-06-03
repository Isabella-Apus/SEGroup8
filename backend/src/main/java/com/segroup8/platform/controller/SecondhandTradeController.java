package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.AuctionCreateRequest;
import com.segroup8.platform.dto.BargainApplyRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.ProductAuctionVO;
import com.segroup8.platform.vo.ProductNegotiationVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secondhand/trade")
public class SecondhandTradeController {

    private final SecondhandTradeService secondhandTradeService;

    public SecondhandTradeController(SecondhandTradeService secondhandTradeService) {
        this.secondhandTradeService = secondhandTradeService;
    }

    @Operation(summary = "买家发起议价")
    @PostMapping("/bargain/apply")
    public Result<ProductNegotiationVO> applyBargain(@Valid @RequestBody BargainApplyRequest request) {
        return Result.success(secondhandTradeService.applyBargain(request));
    }

    @Operation(summary = "卖家确认议价")
    @PostMapping("/bargain/confirm")
    public Result<ProductNegotiationVO> confirmBargain(@Valid @RequestBody BargainConfirmRequest request) {
        return Result.success(secondhandTradeService.confirmBargain(request));
    }

    @Operation(summary = "卖家驳回议价")
    @PostMapping("/bargain/{negotiationId}/reject")
    public Result<ProductNegotiationVO> rejectBargain(@PathVariable Long negotiationId) {
        return Result.success(secondhandTradeService.rejectBargain(negotiationId));
    }

    @Operation(summary = "分页查看我的议价")
    @GetMapping("/bargain/list")
    public Result<PageVO<ProductNegotiationVO>> pageMyBargains(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long counterpartUserId,
            @RequestParam(required = false) String status) {
        return Result.success(secondhandTradeService.pageMyBargains(
                pageNum,
                pageSize,
                productId,
                counterpartUserId,
                status));
    }

    @Operation(summary = "查询我对某二手商品的有效议价")
    @GetMapping("/bargain/effective")
    public Result<ProductNegotiationVO> getMyEffectiveBargain(@RequestParam Long productId) {
        return Result.success(secondhandTradeService.getMyEffectiveNegotiation(productId));
    }

    @Operation(summary = "卖家创建拍卖")
    @PostMapping("/auction")
    public Result<ProductAuctionVO> createAuction(@Valid @RequestBody AuctionCreateRequest request) {
        return Result.success(secondhandTradeService.createAuction(request));
    }

    @Operation(summary = "查看商品拍卖详情")
    @GetMapping("/auction/product/{productId}")
    public Result<ProductAuctionVO> getAuctionByProductId(@PathVariable Long productId) {
        return Result.success(secondhandTradeService.getAuctionByProductId(productId));
    }

    @Operation(summary = "分页查看我发起的拍卖")
    @GetMapping("/auction/seller/list")
    public Result<PageVO<ProductAuctionVO>> pageMyAuctions(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(secondhandTradeService.pageMyAuctions(pageNum, pageSize, status));
    }

    @Operation(summary = "卖家提前结束拍卖")
    @PostMapping("/auction/{auctionId}/close")
    public Result<ProductAuctionVO> closeAuctionEarly(@PathVariable Long auctionId) {
        return Result.success(secondhandTradeService.closeAuctionEarly(auctionId));
    }

    @Operation(summary = "卖家将拍卖标记为流拍")
    @PostMapping("/auction/{auctionId}/flow")
    public Result<ProductAuctionVO> markAuctionFlow(@PathVariable Long auctionId) {
        return Result.success(secondhandTradeService.markAuctionFlow(auctionId));
    }

    @Operation(summary = "参与竞价")
    @PostMapping("/auction/{auctionId}/bid")
    public Result<ProductAuctionVO> placeBid(@PathVariable Long auctionId, @Valid @RequestBody AuctionBidRequest request) {
        return Result.success(secondhandTradeService.placeBid(auctionId, request));
    }
}
