package com.segroup8.platform.service;

import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.AuctionCreateRequest;
import com.segroup8.platform.dto.BargainApplyRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductAuctionVO;
import com.segroup8.platform.vo.ProductNegotiationVO;

import java.math.BigDecimal;

public interface SecondhandTradeService {

    ProductNegotiationVO applyBargain(BargainApplyRequest request);

    ProductNegotiationVO confirmBargain(BargainConfirmRequest request);

    ProductNegotiationVO rejectBargain(Long negotiationId);

    PageVO<ProductNegotiationVO> pageMyBargains(Long pageNum, Long pageSize, Long productId,
            Long counterpartUserId, String status);

    ProductNegotiationVO getMyEffectiveNegotiation(Long productId);

    BigDecimal resolveEffectivePriceForBuyer(Long productId, Long buyerUserId);

    void markNegotiationUsed(Long productId, Long buyerUserId, Long orderId);

    ProductAuctionVO createAuction(AuctionCreateRequest request);

    ProductAuctionVO getAuctionByProductId(Long productId);

    PageVO<ProductAuctionVO> pageMyAuctions(Long pageNum, Long pageSize, String status);

    ProductAuctionVO closeAuctionEarly(Long auctionId);

    ProductAuctionVO markAuctionFlow(Long auctionId);

    ProductAuctionVO placeBid(Long auctionId, AuctionBidRequest request);

    void settleExpiredAuctions();
}
