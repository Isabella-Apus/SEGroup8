package com.segroup8.finance;

import java.util.Map;
import java.util.Locale;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class InfrastructureConfig {
    @Bean("outboxRestClient")
    RestClient outboxRestClient(RestClient.Builder builder,
            @Value("${app.http-connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${app.http-read-timeout-ms:3000}") long readTimeoutMs) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean("secrets")
    HealthIndicator secretsHealth(@Value("${app.internal-service-token}") String token,
            @Value("${app.jwt-secret}") String jwtSecret) {
        return () -> unsafe(token, 16) || unsafe(jwtSecret, 32)
                ? Health.down().withDetails(Map.of("reason", "INTERNAL_SERVICE_TOKEN/JWT_SECRET format is invalid")).build()
                : Health.up().build();
    }

    @Bean("financeSchema")
    HealthIndicator financeSchemaHealth(JdbcTemplate db) {
        return () -> {
            try {
                Integer migrations = db.queryForObject(
                        "select count(*) from flyway_schema_history where success=true", Integer.class);
                db.queryForObject("select count(*) from balance", Integer.class);
                return migrations != null && migrations > 0
                        ? Health.up().withDetail("migration", "validated").withDetail("balanceRead", "ok").build()
                        : Health.down().withDetail("reason", "no successful Flyway migration found").build();
            } catch (RuntimeException failure) {
                return Health.down().withDetail("reason", "finance schema or balance read unavailable").build();
            }
        };
    }

    private static boolean unsafe(String secret, int minimumBytes) {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < minimumBytes) return true;
        String normalized = secret.toLowerCase(Locale.ROOT);
        return normalized.contains("change-me") || normalized.contains("demo_secret");
    }
}
