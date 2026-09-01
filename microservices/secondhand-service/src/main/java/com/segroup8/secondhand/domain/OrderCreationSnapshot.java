package com.segroup8.secondhand.domain;

public record OrderCreationSnapshot(Long addressId, String productName, String receiverName,
        String receiverPhone, String receiverProvince, String receiverCity,
        String receiverDetailAddress) {
}
