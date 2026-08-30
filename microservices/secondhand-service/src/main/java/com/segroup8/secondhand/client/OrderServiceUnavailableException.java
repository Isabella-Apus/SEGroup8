package com.segroup8.secondhand.client;

public class OrderServiceUnavailableException extends RuntimeException {
    public OrderServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderServiceUnavailableException(String message) {
        super(message);
    }
}
