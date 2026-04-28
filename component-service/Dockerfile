# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
COPY common-models common-models
COPY component-service component-service

RUN mvn clean package -DskipTests -pl common-models,component-service -am

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /build/component-service/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
