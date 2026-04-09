package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复最多500字")
    private String reply;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
