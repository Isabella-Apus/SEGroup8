FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY behavior-service/pom.xml behavior-service/pom.xml
COPY behavior-service/src behavior-service/src
RUN mvn -B -pl behavior-service -am -DskipTests package
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/behavior-service/target/behavior-service-1.0.0.jar /app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app.jar"]
