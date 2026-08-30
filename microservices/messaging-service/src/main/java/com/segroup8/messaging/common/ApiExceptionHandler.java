package com.segroup8.messaging.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResult<Void>> api(ApiException ex) {
        return ResponseEntity.status(httpStatus(ex.code())).body(ApiResult.failure(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResult<Void>> validation(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException invalid
                && invalid.getBindingResult().getFieldError() != null
                ? invalid.getBindingResult().getFieldError().getDefaultMessage() : "Invalid request";
        return ResponseEntity.badRequest().body(ApiResult.failure(400, message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResult<Void>> unexpected(Exception ex) {
        return ResponseEntity.internalServerError().body(ApiResult.failure(500, "Internal server error"));
    }

    private HttpStatus httpStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
