package com.segroup8.secondhand.client;

public interface IdentityGateway {
    AddressSnapshot resolveAddress(long userId, Long addressId);

    record AddressSnapshot(long addressId, long userId, String receiverName, String receiverPhone,
            String province, String city, String detailAddress) {
    }
}
