package com.segroup8.secondhand.common;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiResponse<Void>> domain(DomainException exception) {
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(codeAsNumber(exception.code()), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception exception) {
        var errors = exception instanceof MethodArgumentNotValidException methodError
                ? methodError.getBindingResult().getFieldErrors()
                : ((BindException) exception).getBindingResult().getFieldErrors();
        String message = errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, "请求参数格式不正确"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        log.error("Unhandled secondhand-service error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(500, "服务暂时不可用，请稍后重试"));
    }

    private int codeAsNumber(String code) {
        if (code == null) {
            return 400;
        }
        if (code.startsWith("AUTH")) return 401;
        if (code.startsWith("FORBIDDEN") || code.startsWith("OWNERSHIP")) return 403;
        if (code.endsWith("NOT_FOUND")) return 404;
        if (code.contains("CONFLICT") || code.contains("SOLD") || code.contains("DUPLICATE")) return 409;
        return 400;
    }
}
