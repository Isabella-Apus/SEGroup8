package com.segroup8.order;

import com.segroup8.order.ApiModels.ApiResponse;
import com.segroup8.order.ApiModels.InternalSnapshot;
import com.segroup8.order.ApiModels.LogisticsView;
import com.segroup8.order.ApiModels.OrderView;
import com.segroup8.order.ApiModels.PageView;
import com.segroup8.order.ApiModels.PublicOrderView;
import com.segroup8.order.ApiModels.ReviewView;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
class OrderLoggingContextAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        observe(body instanceof ApiResponse<?> apiResponse ? apiResponse.data() : body);
        return body;
    }

    private void observe(Object value) {
        if (value instanceof PublicOrderView order) {
            order(order.id(), order.orderNo());
        } else if (value instanceof OrderView order) {
            order(order.id(), order.orderNo());
        } else if (value instanceof InternalSnapshot snapshot) {
            order(snapshot.orderId(), snapshot.orderNo());
        } else if (value instanceof LogisticsView logistics) {
            MDC.put("orderId", Long.toString(logistics.orderId()));
        } else if (value instanceof ReviewView review) {
            MDC.put("orderId", Long.toString(review.orderId()));
        } else if (value instanceof PageView<?> page) {
            List<?> records = page.records();
            if (records != null && records.size() == 1) observe(records.get(0));
        }
    }

    private void order(long orderId, String orderNo) {
        MDC.put("orderId", Long.toString(orderId));
        if (orderNo != null && !orderNo.isBlank()) MDC.put("orderNo", orderNo);
    }
}
