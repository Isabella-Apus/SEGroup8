FROM eclipse-temurin:17-jre-alpine

ARG GIT_SHA=unknown
ARG JAR_SHA256=unknown
ARG IMAGE_SOURCE=https://github.com/Isabella-Apus/SEGroup8

LABEL org.opencontainers.image.title="Kinda Goods catalog-shop-service" \
      org.opencontainers.image.revision="$GIT_SHA" \
      org.opencontainers.image.source="$IMAGE_SOURCE" \
      com.segroup8.catalog-shop.jar-sha256="$JAR_SHA256"

RUN addgroup -S -g 10001 app && adduser -S -D -u 10001 -G app app
WORKDIR /app
COPY --chown=10001:10001 target/catalog-shop-service-*.jar app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
