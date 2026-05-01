package com.segroup8.platform.service.settlement;

import org.springframework.stereotype.Component;

@Component
public class NewProductSettlementStrategy implements OrderSettlementStrategy {

    @Override
    public boolean supports(String productType) {
        return "NEW".equalsIgnoreCase(productType);
    }

    @Override
    public SettlementAccountType accountType() {
        return SettlementAccountType.BUSINESS;
    }

    @Override
    public String changeType() {
        return "ESCROW_RELEASE_BUSINESS";
    }
}
