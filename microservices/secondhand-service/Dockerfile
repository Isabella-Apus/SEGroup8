FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY shop-service/pom.xml shop-service/pom.xml
COPY risk-service/pom.xml risk-service/pom.xml
COPY behavior-service/pom.xml behavior-service/pom.xml
COPY security-contract/pom.xml security-contract/pom.xml
COPY security-contract/src security-contract/src
COPY secondhand-service/pom.xml secondhand-service/pom.xml
COPY secondhand-service/src secondhand-service/src
RUN mvn -B -pl secondhand-service -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --uid 10001 --create-home appuser
USER appuser
COPY --from=build /workspace/secondhand-service/target/secondhand-service-1.0.0.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app.jar"]
