package com.segroup8.platform.vo;

import java.math.BigDecimal;

public class FinanceDashboardVO {

    private BigDecimal personalBalance;
    private BigDecimal businessBalance;

    public BigDecimal getPersonalBalance() {
        return personalBalance;
    }

    public void setPersonalBalance(BigDecimal personalBalance) {
        this.personalBalance = personalBalance;
    }

    public BigDecimal getBusinessBalance() {
        return businessBalance;
    }

    public void setBusinessBalance(BigDecimal businessBalance) {
        this.businessBalance = businessBalance;
    }
}
