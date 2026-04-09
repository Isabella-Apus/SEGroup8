package com.segroup8.platform.interceptor;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.entity.IdempotencyRecord;
import com.segroup8.platform.mapper.IdempotencyRecordMapper;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;

@ControllerAdvice(annotations = Controller.class)
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final int STATUS_SUCCESS = 1;
    private final IdempotencyRecordMapper idempotencyRecordMapper;
    private final ObjectMapper objectMapper;

    public IdempotencyResponseAdvice(IdempotencyRecordMapper idempotencyRecordMapper, ObjectMapper objectMapper) {
        this.idempotencyRecordMapper = idempotencyRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType, @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        if (!(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }
        Object recordIdObj = servletRequest.getServletRequest().getAttribute(IdempotencyInterceptor.IDEMPOTENCY_RECORD_ID_ATTR);
        if (!(recordIdObj instanceof Long recordId)) {
            return body;
        }
        if (!(body instanceof Result<?>)) {
            return body;
        }
        try {
            String responseBody = objectMapper.writeValueAsString(body);
            idempotencyRecordMapper.update(null, new UpdateWrapper<IdempotencyRecord>()
                    .set("status", STATUS_SUCCESS)
                    .set("http_status", servletResponse.getServletResponse().getStatus())
                    .set("response_body", responseBody)
                    .set("update_time", LocalDateTime.now())
                    .eq("id", recordId));
        } catch (JsonProcessingException ignored) {
            // 序列化失败时不影响主链路
        }
        return body;
    }
}

