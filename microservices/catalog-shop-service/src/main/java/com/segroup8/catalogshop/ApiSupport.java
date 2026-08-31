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

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String,Object>> domain(ApiException e) {
        return ResponseEntity.status(e.status).body(Map.of("code",e.code,"message",e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> validation(MethodArgumentNotValidException e) {
        var fields=new LinkedHashMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(),error.getDefaultMessage()));
        return Map.of("code","VALIDATION_FAILED","message","请求参数校验失败","fields",fields);
    }
}
