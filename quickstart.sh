#!/bin/bash
# Quick Start Guide for Industry Simulator

echo "🚀 Industry Simulator - Quick Start"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven found${NC}"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker found${NC}"

echo ""
echo -e "${YELLOW}Step 1: Building Maven modules...${NC}"
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Maven build successful${NC}"
else
    echo -e "${RED}✗ Maven build failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Building Docker images...${NC}"
echo "This requires sudo and may take 3-5 minutes on first run"
sudo docker compose build --no-cache

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Docker build successful${NC}"
else
    echo -e "${RED}✗ Docker build failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 3: Starting services...${NC}"
echo "Services will start in 15-30 seconds"
sudo docker compose up -d

# Wait for services
sleep 15

echo ""
echo -e "${GREEN}✓ Services started!${NC}"
echo ""
echo "📍 Service URLs:"
echo "   Raw Material Service:  http://localhost:8081/api/raw-materials/health"
echo "   Processing Service:    http://localhost:8082/health"
echo "   Component Service:     http://localhost:8083/health"
echo "   Assembly Service:      http://localhost:8084/health"
echo ""
echo "🗄️  PostgreSQL: localhost:5432"
echo "📨 Kafka: localhost:9092"
echo ""
echo "View logs:"
echo "   sudo docker compose logs -f [service-name]"
echo ""
echo "Stop services:"
echo "   sudo docker compose down"
