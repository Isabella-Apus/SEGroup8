package com.segroup8.messaging.access;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MonolithGovernanceBlockAdapter implements GovernanceBlockPort {
    private final RestClient client;
    public MonolithGovernanceBlockAdapter(RestClient.Builder builder, @Value("${app.governance.base-url:}") String baseUrl) {
        this.client = baseUrl == null || baseUrl.isBlank() ? null : builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<Boolean> isCommunicationBlocked(long actorUserId, long targetUserId, String bearerToken) {
        if (client == null || bearerToken == null || bearerToken.isBlank()) return Optional.empty();
        try {
            Boolean actorBlocks = result("/api/report-block/block/check/{id}", targetUserId, bearerToken);
            Boolean targetBlocks = result("/api/report-block/block/blocked-by/{id}", targetUserId, bearerToken);
            return actorBlocks == null || targetBlocks == null ? Optional.empty() : Optional.of(actorBlocks || targetBlocks);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Boolean result(String path, long id, String bearerToken) {
        Map<String, Object> body = client.get().uri(path, id).header("Authorization", "Bearer " + bearerToken)
                .retrieve().body(Map.class);
        if (body == null || !(body.get("data") instanceof Boolean value)) return null;
        return value;
    }
}
