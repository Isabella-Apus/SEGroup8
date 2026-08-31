package com.segroup8.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the HTTP idempotency contract independently from business request IDs.
 * A supplied key is scoped to one caller and operation, and can never be reused
 * for a different request body.
 */
@Service
class IdempotencyKeyService {
    private final JdbcClient db;
    private final ObjectMapper json;

    IdempotencyKeyService(JdbcClient db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    @Transactional
    <T> T execute(String scope, String key, Object request, Class<T> resultType, Supplier<T> action) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) return action.get();
        String fingerprint = fingerprint(request);
        Optional<Row> existing = lookup(scope, normalizedKey);
        if (existing.isPresent()) return replay(existing.get(), scope, normalizedKey, fingerprint, resultType);
        try {
            db.sql("insert into idempotency_record(scope,request_key,request_fingerprint,response_type) "
                            + "values(:scope,:key,:fingerprint,:type)")
                    .param("scope", scope).param("key", normalizedKey).param("fingerprint", fingerprint)
                    .param("type", resultType.getName()).update();
        } catch (DataIntegrityViolationException concurrent) {
            return replay(lookup(scope, normalizedKey).orElseThrow(() -> concurrent), scope, normalizedKey,
                    fingerprint, resultType);
        }
        T result = action.get();
        db.sql("update idempotency_record set response_body=:response where scope=:scope and request_key=:key")
                .param("scope", scope).param("key", normalizedKey).param("response", serialize(result)).update();
        return result;
    }

    @Transactional
    void executeVoid(String scope, String key, Object request, Runnable action) {
        execute(scope, key, request, VoidResult.class, () -> {
            action.run();
            return VoidResult.INSTANCE;
        });
    }

    private <T> T replay(Row row, String scope, String key, String fingerprint, Class<T> resultType) {
        if (!fingerprint.equals(row.fingerprint())) {
            throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED",
                    "同一 Idempotency-Key 不能用于不同的请求参数");
        }
        if (row.responseBody() == null) {
            throw DomainException.conflict("IDEMPOTENCY_REQUEST_IN_PROGRESS", "相同 Idempotency-Key 的请求仍在处理中");
        }
        try {
            return json.readValue(row.responseBody(), resultType);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("stored idempotency response cannot be decoded for " + scope + ":" + key,
                    error);
        }
    }

    private Optional<Row> lookup(String scope, String key) {
        return db.sql("select request_fingerprint,response_body from idempotency_record "
                        + "where scope=:scope and request_key=:key for update")
                .param("scope", scope).param("key", key)
                .query((rs, row) -> new Row(rs.getString(1), rs.getString(2))).optional();
    }

    private String fingerprint(Object request) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request)));
        } catch (NoSuchAlgorithmException | JsonProcessingException error) {
            throw new IllegalStateException("cannot fingerprint idempotency request", error);
        }
    }

    private String serialize(Object result) {
        try {
            return json.writeValueAsString(result);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("cannot persist idempotency response", error);
        }
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.trim();
        if (!normalized.matches("[A-Za-z0-9._:-]{1,80}")) {
            throw DomainException.badRequest("IDEMPOTENCY_KEY_INVALID", "Idempotency-Key 格式不合法");
        }
        return normalized;
    }

    private static String hex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) builder.append(String.format("%02x", item));
        return builder.toString();
    }

    private record Row(String fingerprint, String responseBody) { }
    private enum VoidResult { INSTANCE }
}
