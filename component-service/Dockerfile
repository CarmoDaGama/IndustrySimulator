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

# Build project with retry logic and increased timeouts
RUN mvn clean package -DskipTests -pl common-models,component-service -am \
    -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.retryHandler.class=standard \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.pool.persistent=false \
    -Dmaven.wagon.rto=300000 || \
    (sleep 10 && mvn clean package -DskipTests -pl common-models,component-service -am \
    -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.retryHandler.class=standard \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.rto=300000)

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /build/component-service/target/component-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
