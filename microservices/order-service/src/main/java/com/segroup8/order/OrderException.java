package com.segroup8.order;

public class OrderException extends RuntimeException {
    private final String code;
    private final int status;

    public OrderException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }
}
