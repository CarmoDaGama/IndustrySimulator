#!/bin/bash
# Quick Start Guide for Industry Simulator
#
# Arquitectura: apenas o Kafka e o servidor de Base de Dados correm em Docker.
# Os microserviços correm nativamente na máquina local (proibido empacotá-los
# em contentores, conforme o enunciado do projecto).

set -e

echo "🚀 Industry Simulator - Quick Start"
echo "===================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven found${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker found${NC}"

echo ""
echo -e "${YELLOW}Step 1: Building Maven modules...${NC}"
mvn clean package -DskipTests -q
echo -e "${GREEN}✓ Maven build successful${NC}"

echo ""
echo -e "${YELLOW}Step 2: Starting infrastructure (Kafka + PostgreSQL only)...${NC}"
docker compose up -d
echo "Waiting for Kafka and PostgreSQL to become healthy..."
sleep 20

echo ""
echo -e "${YELLOW}Step 3: Starting microservices natively (java -jar)...${NC}"
mkdir -p logs
declare -A JARS=(
  [raw-material-service]=raw-material-service/target/raw-material-service-*.jar
  [processing-service]=processing-service/target/processing-service-*.jar
  [component-service]=component-service/target/component-service-*.jar
  [assembly-service]=assembly-service/target/assembly-service-*.jar
)

for svc in "${!JARS[@]}"; do
  jar=$(ls ${JARS[$svc]} 2>/dev/null | head -1)
  if [ -z "$jar" ]; then
    echo -e "${RED}✗ JAR not found for $svc${NC}"
    continue
  fi
  echo "Starting $svc ($jar)..."
  nohup java -jar "$jar" > "logs/$svc.log" 2>&1 &
  echo $! > "logs/$svc.pid"
done

sleep 10

echo ""
echo -e "${GREEN}✓ Services started!${NC}"
echo ""
echo "📍 Service URLs:"
echo "   Raw Material Service:  http://localhost:8081/api/raw-materials/health"
echo "   Processing Service:    http://localhost:8082/api/processing/health"
echo "   Component Service:     http://localhost:8083/api/components/health"
echo "   Assembly Service:      http://localhost:8084/api/inventory/health"
echo ""
echo "🗄️  PostgreSQL (servidor único, 4 bases de dados isoladas): localhost:5432"
echo "📨 Kafka: localhost:9092"
echo ""
echo "Ver logs:      tail -f logs/<service>.log"
echo "Parar serviços: kill \$(cat logs/*.pid)"
echo "Parar infra:    docker compose down"
