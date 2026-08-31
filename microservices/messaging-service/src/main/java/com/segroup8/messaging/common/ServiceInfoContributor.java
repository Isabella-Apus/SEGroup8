package com.segroup8.messaging.common;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** Exposes release identity without exposing any runtime secret. */
@Component
public class ServiceInfoContributor implements InfoContributor {
    private final String version;
    private final String commit;
    private final String buildTime;

    public ServiceInfoContributor(@Value("${APP_VERSION:dev}") String version,
            @Value("${APP_COMMIT:unknown}") String commit,
            @Value("${APP_BUILD_TIME:unknown}") String buildTime) {
        this.version = version;
        this.commit = commit;
        this.buildTime = buildTime;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("service", Map.of("name", "messaging-service", "version", version,
                "commit", commit, "buildTime", buildTime));
    }
}
