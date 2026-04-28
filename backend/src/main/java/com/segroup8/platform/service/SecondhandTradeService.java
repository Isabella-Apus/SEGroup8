package com.segroup8.platform.service;

import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.AuctionCreateRequest;
import com.segroup8.platform.dto.BargainApplyRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.vo.ProductAuctionVO;
import com.segroup8.platform.vo.ProductNegotiationVO;

import java.math.BigDecimal;

public interface SecondhandTradeService {

    ProductNegotiationVO applyBargain(BargainApplyRequest request);

    ProductNegotiationVO confirmBargain(BargainConfirmRequest request);

    ProductNegotiationVO getMyEffectiveNegotiation(Long productId);

    BigDecimal resolveEffectivePriceForBuyer(Long productId, Long buyerUserId);

    void markNegotiationUsed(Long productId, Long buyerUserId, Long orderId);

    ProductAuctionVO createAuction(AuctionCreateRequest request);

    ProductAuctionVO getAuctionByProductId(Long productId);

    ProductAuctionVO placeBid(Long auctionId, AuctionBidRequest request);

    void settleExpiredAuctions();
}
