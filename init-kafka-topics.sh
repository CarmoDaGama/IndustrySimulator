#!/bin/bash

# Wait for Kafka to be ready
sleep 10

# Create Kafka topics
docker exec IndustrySimulator-kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic raw-material-produced --partitions 1 --replication-factor 1 2>/dev/null || true

docker exec IndustrySimulator-kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic processing-completed --partitions 1 --replication-factor 1 2>/dev/null || true

docker exec IndustrySimulator-kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic component-assembled --partitions 1 --replication-factor 1 2>/dev/null || true

docker exec IndustrySimulator-kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic product-assembled --partitions 1 --replication-factor 1 2>/dev/null || true

docker exec IndustrySimulator-kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic inventory-updated --partitions 1 --replication-factor 1 2>/dev/null || true

echo "Kafka topics created successfully!"
