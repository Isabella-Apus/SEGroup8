package com.segroup8.secondhand.client;

public interface AddressGateway {
    AddressSnapshot requireOwnedAddress(long userId, long addressId, String requestId);

    record AddressSnapshot(String receiverName, String receiverPhone, String province,
            String city, String detailAddress) {
    }
}
