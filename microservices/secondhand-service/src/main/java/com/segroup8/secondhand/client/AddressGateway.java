package com.segroup8.secondhand.client;

public interface AddressGateway {
    AddressSnapshot requireOwnedAddress(long userId, long addressId, String requestId);

    default AddressSnapshot requireDefaultAddress(long userId, String requestId) {
        throw new UnsupportedOperationException("default address lookup is not configured");
    }

    record AddressSnapshot(String receiverName, String receiverPhone, String province,
            String city, String detailAddress) {
    }
}
