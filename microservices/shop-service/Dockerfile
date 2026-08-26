FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY shop-service/pom.xml shop-service/pom.xml
COPY shop-service/src shop-service/src
RUN mvn -B -pl shop-service -am -DskipTests package
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/shop-service/target/shop-service-1.0.0.jar /app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app.jar"]
