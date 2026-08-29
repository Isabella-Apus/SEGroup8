package com.segroup8.identity.api;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResult<Void>> handleApi(ApiException ex) {
        return ResponseEntity.ok(ApiResult.failure(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        return ResponseEntity.ok(ApiResult.failure(400, error == null ? "请求参数不合法" : error.getDefaultMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiResult<Void>> handleDuplicate(DuplicateKeyException ex) {
        return ResponseEntity.ok(ApiResult.failure(400, "数据已存在"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class, ClassCastException.class, NullPointerException.class})
    ResponseEntity<ApiResult<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.ok(ApiResult.failure(400, "请求参数不合法"));
    }
}
