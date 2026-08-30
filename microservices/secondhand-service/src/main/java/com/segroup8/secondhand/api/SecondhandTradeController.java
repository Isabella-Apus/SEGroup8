package com.segroup8.secondhand.api;

import static com.segroup8.secondhand.security.AuthenticationSupport.requireUser;

import com.segroup8.secondhand.common.ApiResponse;
import com.segroup8.secondhand.common.PageResponse;
import com.segroup8.secondhand.security.AuthenticatedUser;
import com.segroup8.secondhand.service.TradeApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secondhand/trade")
public class SecondhandTradeController {
    private final TradeApplicationService trades;

    public SecondhandTradeController(TradeApplicationService trades) {
        this.trades = trades;
    }

    @Operation(summary = "UC18 买家发起议价")
    @PostMapping("/bargain/apply")
    ApiResponse<NegotiationView> apply(HttpServletRequest request,
            @Valid @RequestBody BargainApplyRequest command) {
        return ApiResponse.success(trades.applyBargain(requireUser(request).userId(), command));
    }

    @Operation(summary = "UC18 卖家同意义价并创建待付款订单")
    @PostMapping("/bargain/confirm")
    ApiResponse<NegotiationView> confirm(HttpServletRequest request,
            @Valid @RequestBody BargainConfirmRequest command) {
        return ApiResponse.success(trades.confirmBargain(requireUser(request).userId(), command));
    }

    @Operation(summary = "UC18 卖家拒绝议价")
    @PostMapping("/bargain/{negotiationId}/reject")
    ApiResponse<NegotiationView> reject(HttpServletRequest request, @PathVariable long negotiationId) {
        return ApiResponse.success(trades.rejectBargain(requireUser(request).userId(), negotiationId));
    }

    @Operation(summary = "UC18 查看与当前用户有关的议价")
    @GetMapping("/bargain/list")
    ApiResponse<PageResponse<NegotiationView>> bargains(HttpServletRequest request,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long counterpartUserId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(trades.listBargains(requireUser(request).userId(), pageNum, pageSize,
                productId, counterpartUserId, status));
    }

    @Operation(summary = "UC18 查询买家的有效议价")
    @GetMapping("/bargain/effective")
    ApiResponse<NegotiationView> effective(HttpServletRequest request, @RequestParam long productId) {
        return ApiResponse.success(trades.effectiveBargain(requireUser(request).userId(), productId));
    }

    @Operation(summary = "UC19 卖家发起拍卖")
    @PostMapping("/auction")
    ApiResponse<AuctionView> createAuction(HttpServletRequest request,
            @Valid @RequestBody AuctionCreateRequest command) {
        return ApiResponse.success(trades.createAuction(requireUser(request).userId(), command));
    }

    @Operation(summary = "UC19 查看商品拍卖详情")
    @GetMapping("/auction/product/{productId}")
    ApiResponse<AuctionView> auction(@PathVariable long productId) {
        return ApiResponse.success(trades.auctionByProduct(productId));
    }

    @Operation(summary = "UC19 卖家查看自己的拍卖")
    @GetMapping("/auction/seller/list")
    ApiResponse<PageResponse<AuctionView>> sellerAuctions(HttpServletRequest request,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(trades.sellerAuctions(requireUser(request).userId(), pageNum, pageSize, status));
    }

    @Operation(summary = "UC19 卖家提前结束并幂等结算拍卖")
    @PostMapping("/auction/{auctionId}/close")
    ApiResponse<AuctionView> close(HttpServletRequest request, @PathVariable long auctionId) {
        return ApiResponse.success(trades.closeAuction(requireUser(request).userId(), auctionId));
    }

    @Operation(summary = "UC19 无出价时标记流拍")
    @PostMapping("/auction/{auctionId}/flow")
    ApiResponse<AuctionView> flow(HttpServletRequest request, @PathVariable long auctionId) {
        return ApiResponse.success(trades.markAuctionFlow(requireUser(request).userId(), auctionId));
    }

    @Operation(summary = "UC19 买家并发安全出价")
    @PostMapping("/auction/{auctionId}/bid")
    ApiResponse<AuctionView> bid(HttpServletRequest request, @PathVariable long auctionId,
            @Valid @RequestBody AuctionBidRequest command) {
        AuthenticatedUser user = requireUser(request);
        return ApiResponse.success(trades.placeBid(user.userId(), user.username(), auctionId, command.bidAmount()));
    }
}
