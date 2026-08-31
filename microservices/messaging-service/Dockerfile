# Build context is the microservices directory, matching the existing service images.
FROM eclipse-temurin:17-jre-alpine

ARG GIT_SHA=unknown
ARG JAR_SHA256=unknown
ARG IMAGE_SOURCE=https://github.com/segroup8/kinda-goods

LABEL org.opencontainers.image.title="Kinda Goods messaging-service" \
      org.opencontainers.image.revision="$GIT_SHA" \
      org.opencontainers.image.source="$IMAGE_SOURCE" \
      com.segroup8.messaging.jar-sha256="$JAR_SHA256"

RUN addgroup -S -g 10001 app \
    && adduser -S -D -u 10001 -G app app

WORKDIR /app
COPY messaging-service/target/messaging-service-1.0.0.jar /app/messaging-service.jar
RUN chown 10001:10001 /app/messaging-service.jar

USER 10001:10001
EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8084/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/messaging-service.jar"]
