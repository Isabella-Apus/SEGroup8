package com.segroup8.messaging.common;

public record ApiResult<T>(int code, String message, T data) {
    public static <T> ApiResult<T> success(T data) { return new ApiResult<>(0, "success", data); }
    public static ApiResult<Void> success() { return new ApiResult<>(0, "success", null); }
    public static ApiResult<Void> failure(int code, String message) { return new ApiResult<>(code, message, null); }
}
