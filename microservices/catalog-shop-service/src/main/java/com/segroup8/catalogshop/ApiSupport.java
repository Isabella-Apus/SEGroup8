package com.segroup8.catalogshop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class ApiException extends RuntimeException {
    final String code; final HttpStatus status;
    ApiException(String code, String message) { this(code, message, HttpStatus.CONFLICT); }
    ApiException(String code, String message, HttpStatus status) { super(message); this.code=code; this.status=status; }
}

record ApiResult<T>(int code, String message, T data, String error) {
    static <T> ApiResult<T> success(T data) { return new ApiResult<>(0, "success", data, null); }
}

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResult<Void>> domain(ApiException e) {
        return ResponseEntity.status(e.status).body(new ApiResult<>(e.status.value(),e.getMessage(),null,e.code));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResult<Map<String,String>> validation(MethodArgumentNotValidException e) {
        var fields=new LinkedHashMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(),error.getDefaultMessage()));
        return new ApiResult<>(400,"请求参数校验失败",fields,"VALIDATION_FAILED");
    }
}
