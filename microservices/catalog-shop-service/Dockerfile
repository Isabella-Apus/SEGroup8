FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY target/catalog-shop-service-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
