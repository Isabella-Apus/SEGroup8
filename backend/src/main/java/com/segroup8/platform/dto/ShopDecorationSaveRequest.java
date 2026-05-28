package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;

public class ShopDecorationSaveRequest {

    @NotBlank(message = "装修内容不能为空")
    private String decorationJson;

    public String getDecorationJson() {
        return decorationJson;
    }

    public void setDecorationJson(String decorationJson) {
        this.decorationJson = decorationJson;
    }
}
