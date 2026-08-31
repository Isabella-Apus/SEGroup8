FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --chown=10001:10001 target/order-service-*.jar app.jar
USER 10001:10001
EXPOSE 8085
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=4 \
  CMD curl --fail --silent http://127.0.0.1:8085/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
