package com.segroup8.platform.service;

import com.segroup8.platform.vo.CreditScoreVO;
import com.segroup8.platform.vo.SellerRatingVO;
import org.springframework.stereotype.Component;

@Component
public class SellerRatingAssembler {

    private final CreditService creditService;

    public SellerRatingAssembler(CreditService creditService) {
        this.creditService = creditService;
    }

    public SellerRatingVO build(Long userId) {
        if (userId == null) {
            return null;
        }
        CreditScoreVO credit = creditService.getCreditInfo(userId);
        SellerRatingVO rating = new SellerRatingVO();
        rating.setOverallScore(credit.getOverallScore());
        rating.setOverallLevel(credit.getOverallLevel());
        rating.setShopScore(credit.getShopScore());
        rating.setShopLevel(credit.getShopLevel());
        rating.setShopSoldCount(credit.getShopSoldCount());
        rating.setShopGoodRate(credit.getShopGoodRate());
        rating.setShSellerScore(credit.getShSellerScore());
        rating.setShSellerLevel(credit.getShSellerLevel());
        rating.setShSellerSoldCount(credit.getShSellerSoldCount());
        rating.setShSellerGoodRate(credit.getShSellerGoodRate());
        return rating;
    }
}
