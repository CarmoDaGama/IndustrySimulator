#!/bin/bash

# ============================================================================
# INDUSTRY SIMULATOR - Kafka Topics Initialization Script
# ============================================================================
# This script creates all required Kafka topics for the Industry Simulator
# microservices. It waits for Kafka to be ready and creates topics with
# proper configurations.

set -e

echo "🚀 Starting Kafka topics initialization..."

# Configuration
KAFKA_CONTAINER="industry-kafka"
BOOTSTRAP_SERVER="kafka:29092"
PARTITIONS=1
REPLICATION_FACTOR=1
RETENTION_MS=604800000  # 7 days

# Define all topics for the system
TOPICS=(
    "raw-material-produced"
    "processing-completed"
    "component-assembled"
    "product-assembled"
    "inventory-updated"
)

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka broker to be ready..."
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server $BOOTSTRAP_SERVER &>/dev/null; then
        echo "✅ Kafka broker is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Kafka broker failed to start after $max_attempts attempts"
    exit 1
fi

# Create topics
echo ""
echo "📝 Creating Kafka topics..."
for topic in "${TOPICS[@]}"; do
    echo "  - Creating topic: $topic"
    docker exec $KAFKA_CONTAINER kafka-topics \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --create \
        --topic $topic \
        --partitions $PARTITIONS \
        --replication-factor $REPLICATION_FACTOR \
        --config retention.ms=$RETENTION_MS \
        --if-not-exists 2>/dev/null || {
            echo "    ⚠️  Topic already exists or error occurred (continuing...)"
        }
done

# Verify topics were created
echo ""
echo "✓ Verifying topics..."
docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --list

echo ""
echo "✅ Kafka topics initialization completed successfully!"
echo ""
