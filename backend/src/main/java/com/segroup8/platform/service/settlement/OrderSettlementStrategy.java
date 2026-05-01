package com.segroup8.platform.service.settlement;

public interface OrderSettlementStrategy {

    boolean supports(String productType);

    SettlementAccountType accountType();

    String changeType();
}
