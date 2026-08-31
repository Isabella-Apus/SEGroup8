package com.segroup8.identity.api;

public record ApiResult<T>(int code, String message, T data) {
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(0, "success", data);
    }

    public static ApiResult<Void> success() {
        return success(null);
    }

    public static ApiResult<Void> failure(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
