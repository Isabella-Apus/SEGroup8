package com.segroup8.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderItemReviewBatchSubmitRequest {

    @Valid
    @NotEmpty(message = "评价明细不能为空")
    private List<OrderItemReviewSubmitRequest> items;

    public List<OrderItemReviewSubmitRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemReviewSubmitRequest> items) {
        this.items = items;
    }
}
