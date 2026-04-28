# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
COPY common-models common-models
COPY raw-material-service raw-material-service

RUN mvn clean package -DskipTests -pl common-models,raw-material-service -am

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /build/raw-material-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
