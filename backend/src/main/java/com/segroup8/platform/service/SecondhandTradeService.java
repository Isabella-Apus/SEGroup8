package com.segroup8.platform.service;

import com.segroup8.platform.vo.PageVO;

import java.util.Map;

public interface SecondhandTradeService {

    Map<String, Object> applyBargain(Map<String, Object> request);

    Map<String, Object> confirmBargain(Map<String, Object> request);

    Map<String, Object> rejectBargain(Long negotiationId);

    PageVO<Map<String, Object>> pageBargains(Long pageNum, Long pageSize, Long productId, Long counterpartUserId, String status);

    Map<String, Object> getMyEffectiveBargain(Long productId);

    Map<String, Object> createAuction(Map<String, Object> request);

    Map<String, Object> getAuctionByProductId(Long productId);

    PageVO<Map<String, Object>> pageMyAuctions(Long pageNum, Long pageSize, String status);

    Map<String, Object> closeAuctionEarly(Long auctionId);

    Map<String, Object> markAuctionFlow(Long auctionId);

    Map<String, Object> placeBid(Long auctionId, Map<String, Object> request);
}
