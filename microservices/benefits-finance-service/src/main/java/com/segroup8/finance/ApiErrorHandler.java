package com.segroup8.finance;

import com.segroup8.finance.ApiModels.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> domain(DomainException error) {
        return ResponseEntity.status(error.status).body(body(error.code, error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField).distinct().collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(body("INVALID_ARGUMENT", "参数不合法: " + message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> invalidParameter(ConstraintViolationException error) {
        return ResponseEntity.badRequest().body(body("INVALID_ARGUMENT", error.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> malformedRequest(Exception error) {
        return ResponseEntity.badRequest().body(body("INVALID_ARGUMENT", "请求 JSON 或参数类型不合法"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataConflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body("DATA_CONFLICT", "请求与现有业务数据冲突"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> missingRoute(NoResourceFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body("RESOURCE_NOT_FOUND", "请求的资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception error) {
        LOG.error("Unhandled request failure", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("INTERNAL_ERROR", "服务暂时不可用，请携带 requestId 重试或查询结果"));
    }

    private ErrorResponse body(String code, String message) {
        return new ErrorResponse(code, message, MDC.get("requestId"), Instant.now());
    }
}
