package com.segroup8.platform.service.settlement;

import org.springframework.stereotype.Component;

@Component
public class SecondhandSettlementStrategy implements OrderSettlementStrategy {

    @Override
    public boolean supports(String productType) {
        return "SECONDHAND".equalsIgnoreCase(productType);
    }

    @Override
    public SettlementAccountType accountType() {
        return SettlementAccountType.PERSONAL;
    }

    @Override
    public String changeType() {
        return "ESCROW_RELEASE_PERSONAL";
    }
}
