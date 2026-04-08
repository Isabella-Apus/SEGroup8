package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRefundApplyRequest {

    @Size(max = 255, message = "退货原因最多255字")
    private String reason;

    private List<String> proofUrls;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getProofUrls() {
        return proofUrls;
    }

    public void setProofUrls(List<String> proofUrls) {
        this.proofUrls = proofUrls;
    }
}
