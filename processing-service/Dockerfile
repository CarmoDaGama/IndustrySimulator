# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy entire project structure
COPY pom.xml .
COPY common-models common-models
COPY raw-material-service raw-material-service
COPY processing-service processing-service
COPY component-service component-service
COPY assembly-service assembly-service

# Build project
RUN mvn clean package -DskipTests -pl common-models,processing-service -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /build/processing-service/target/processing-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
