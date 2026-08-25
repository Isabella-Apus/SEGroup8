FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY risk-service/pom.xml risk-service/pom.xml
COPY risk-service/src risk-service/src
RUN mvn -B -pl risk-service -am -DskipTests package
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/risk-service/target/risk-service-1.0.0.jar /app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app.jar"]
