package com.segroup8.platform.interceptor;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.IdempotencyRecord;
import com.segroup8.platform.mapper.IdempotencyRecordMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    public static final String IDEMPOTENCY_RECORD_ID_ATTR = "IDEMPOTENCY_RECORD_ID";
    private static final long TTL_MILLIS = 10_000L;
    private static final int STATUS_PROCESSING = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final String IDEMPOTENCY_REPLAY_HEADER = "X-Idempotency-Replay";
    private final IdempotencyRecordMapper idempotencyRecordMapper;

    public IdempotencyInterceptor(IdempotencyRecordMapper idempotencyRecordMapper) {
        this.idempotencyRecordMapper = idempotencyRecordMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (CorsUtils.isPreFlightRequest(request) || "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String idemKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (!StringUtils.hasText(idemKey)) {
            return true;
        }
        Long uid = UserContext.getUserId();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String normalizedKey = idemKey.trim();
        IdempotencyRecord existing = findRecord(uid, method, path, normalizedKey);
        if (existing != null && existing.getExpireTime() != null && existing.getExpireTime().isAfter(LocalDateTime.now())) {
            return handleDuplicate(existing, response);
        }
        LocalDateTime now = LocalDateTime.now();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setUserId(uid);
        record.setRequestMethod(method);
        record.setRequestPath(path);
        record.setIdempotencyKey(normalizedKey);
        record.setStatus(STATUS_PROCESSING);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setExpireTime(now.plusNanos(TTL_MILLIS * 1_000_000));
        try {
            idempotencyRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord duplicate = findRecord(uid, method, path, normalizedKey);
            if (duplicate != null) {
                return handleDuplicate(duplicate, response);
            }
            throw new BusinessException(409, "请求重复，请勿重复提交");
        }
        request.setAttribute(IDEMPOTENCY_RECORD_ID_ATTR, record.getId());
        return true;
    }

    private IdempotencyRecord findRecord(Long uid, String method, String path, String key) {
        return idempotencyRecordMapper.selectOne(new LambdaQueryWrapper<IdempotencyRecord>()
                .eq(IdempotencyRecord::getUserId, uid)
                .eq(IdempotencyRecord::getRequestMethod, method)
                .eq(IdempotencyRecord::getRequestPath, path)
                .eq(IdempotencyRecord::getIdempotencyKey, key)
                .last("limit 1"));
    }

    private boolean handleDuplicate(IdempotencyRecord existing, HttpServletResponse response) {
        if (existing.getStatus() != null
                && existing.getStatus() == STATUS_SUCCESS
                && StringUtils.hasText(existing.getResponseBody())) {
            try {
                response.setStatus(existing.getHttpStatus() == null ? HttpServletResponse.SC_OK : existing.getHttpStatus());
                response.setCharacterEncoding("UTF-8");
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader(IDEMPOTENCY_REPLAY_HEADER, "SUCCESS");
                response.getWriter().write(existing.getResponseBody());
                response.getWriter().flush();
                return false;
            } catch (IOException e) {
                throw new BusinessException(500, "幂等结果回放失败");
            }
        }
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(IDEMPOTENCY_REPLAY_HEADER, "PROCESSING");
            response.getWriter().write("{\"code\":0,\"message\":\"请求已受理处理中，请稍后刷新查看结果\",\"data\":null}");
            response.getWriter().flush();
            return false;
        } catch (IOException e) {
            throw new BusinessException(500, "幂等处理中响应失败");
        }
    }
}

