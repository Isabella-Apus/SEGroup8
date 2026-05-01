package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRefundApplyRequest {

    @Size(max = 255, message = "退货原因最多255字")
    private String reason;

    /**
     * ONLY_REFUND: 仅退款
     * RETURN_REFUND: 退货退款
     */
    private String refundMode;

    private List<String> proofUrls;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRefundMode() {
        return refundMode;
    }

    public void setRefundMode(String refundMode) {
        this.refundMode = refundMode;
    }

    public List<String> getProofUrls() {
        return proofUrls;
    }

    public void setProofUrls(List<String> proofUrls) {
        this.proofUrls = proofUrls;
    }
}
