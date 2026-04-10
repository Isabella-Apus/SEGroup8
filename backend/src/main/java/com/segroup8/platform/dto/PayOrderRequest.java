package com.segroup8.platform.dto;

public class PayOrderRequest {

    /**
     * COIN: 商城币支付
     * THIRD_PARTY: 第三方支付
     */
    private String payMode;

    /**
     * WECHAT / ALIPAY
     */
    private String payChannel;

    public String getPayMode() {
        return payMode;
    }

    public void setPayMode(String payMode) {
        this.payMode = payMode;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(String payChannel) {
        this.payChannel = payChannel;
    }
}
