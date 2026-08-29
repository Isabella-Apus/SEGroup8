FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app app
COPY target/identity-governance-service-*.jar app.jar
USER app
EXPOSE 8091
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8091/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
