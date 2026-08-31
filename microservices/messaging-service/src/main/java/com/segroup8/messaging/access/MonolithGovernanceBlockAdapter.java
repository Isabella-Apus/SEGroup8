package com.segroup8.messaging.access;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Reads block state from identity-governance's service-only API. */
@Component
public class MonolithGovernanceBlockAdapter implements GovernanceBlockPort {
    private final RestClient client;
    private final String serviceToken;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public MonolithGovernanceBlockAdapter(RestClient.Builder builder,
            @Value("${app.governance.identity-base-url:}") String baseUrl,
            @Value("${app.governance.service-token:}") String serviceToken,
            @Value("${app.governance.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.governance.read-timeout-ms:3000}") int readTimeoutMs,
            @Value("${app.governance.max-attempts:3}") int maxAttempts,
            @Value("${app.governance.retry-backoff-ms:100}") long retryBackoffMs) {
        this.serviceToken = serviceToken == null ? "" : serviceToken.trim();
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        if (baseUrl == null || baseUrl.isBlank() || this.serviceToken.isBlank()) {
            this.client = null;
        } else {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Math.max(1, connectTimeoutMs));
            requestFactory.setReadTimeout(Math.max(1, readTimeoutMs));
            this.client = builder.requestFactory(requestFactory).baseUrl(baseUrl.trim()).build();
        }
    }

    @Override
    public Optional<Boolean> isCommunicationBlocked(long actorUserId, long targetUserId) {
        if (client == null) return Optional.empty();
        try {
            return parseBlockState(call(actorUserId, targetUserId), actorUserId, targetUserId);
        } catch (RuntimeException ex) {
            // BlockPolicy fails closed when identity is unavailable.
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(long actorUserId, long targetUserId) {
        List<Map<String, Object>> pairs = List.of(
                Map.of("blockerId", actorUserId, "blockedId", targetUserId),
                Map.of("blockerId", targetUserId, "blockedId", actorUserId));
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.post()
                        .uri("/internal/blocks/check")
                        .header("X-Internal-Service-Token", serviceToken)
                        .header("X-Service-Identity", "messaging-service")
                        .header("X-Request-Id", UUID.randomUUID().toString())
                        .header("X-Idempotency-Key", "messaging-block-check-" + UUID.randomUUID())
                        .body(Map.of("pairs", pairs))
                        .retrieve()
                        .body(Map.class);
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                lastFailure = ex;
                if (attempt < maxAttempts) pauseBeforeRetry(attempt);
            }
        }
        throw lastFailure == null ? new IllegalStateException("identity block check failed") : lastFailure;
    }

    private Optional<Boolean> parseBlockState(Map<String, Object> body, long actorUserId, long targetUserId) {
        if (body == null || !(body.get("code") instanceof Number code) || code.intValue() != 0) {
            return Optional.empty();
        }
        Object data = body.get("data");
        if (!(data instanceof List<?> rows)) return Optional.empty();
        boolean actorPairSeen = false;
        boolean targetPairSeen = false;
        boolean blocked = false;
        for (Object value : rows) {
            if (!(value instanceof Map<?, ?> row)
                    || !(row.get("blockerId") instanceof Number blocker)
                    || !(row.get("blockedId") instanceof Number blockedUser)
                    || !(row.get("blocked") instanceof Boolean pairBlocked)) {
                return Optional.empty();
            }
            long blockerId = blocker.longValue();
            long blockedId = blockedUser.longValue();
            if (blockerId == actorUserId && blockedId == targetUserId) {
                actorPairSeen = true;
            } else if (blockerId == targetUserId && blockedId == actorUserId) {
                targetPairSeen = true;
            } else {
                return Optional.empty();
            }
            blocked |= pairBlocked;
        }
        return actorPairSeen && targetPairSeen ? Optional.of(blocked) : Optional.empty();
    }

    private void pauseBeforeRetry(int attempt) {
        if (retryBackoffMs == 0) return;
        try {
            Thread.sleep(Math.min(1000L, retryBackoffMs * attempt));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("identity block check retry interrupted", ex);
        }
    }
}
