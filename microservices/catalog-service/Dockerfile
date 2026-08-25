FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY catalog-service/src catalog-service/src
RUN mvn -B -pl catalog-service -am -DskipTests package
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/catalog-service/target/catalog-service-1.0.0.jar /app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
