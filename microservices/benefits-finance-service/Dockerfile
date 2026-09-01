FROM eclipse-temurin:17.0.20_8-jre-jammy@sha256:e17d77fb030dd4b642dc078d048a5fb9efcb3676ee20305d905949105a6ccd5a

ARG VCS_REF=unknown
ARG BUILD_TIME=unknown
ARG JAR_SHA256=unknown
LABEL org.opencontainers.image.title="benefits-finance-service" \
      org.opencontainers.image.source="https://github.com/Isabella-Apus/SEGroup8" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_TIME}" \
      com.segroup8.jar.sha256="${JAR_SHA256}"

RUN groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid 10001 --no-create-home appuser
WORKDIR /app
COPY --chown=10001:10001 target/benefits-finance-service-*.jar app.jar
USER 10001:10001
EXPOSE 8085
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=6 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8085; printf 'GET /actuator/health/liveness HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3; grep -q '\"status\":\"UP\"' <&3"]
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
