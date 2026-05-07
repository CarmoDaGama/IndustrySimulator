# 🏭 Industry Simulator - Quick Start Guide

Complete setup instructions to run the entire Industry Simulator system with backend microservices, Kafka, databases, and Angular frontend.

## 📋 Prerequisites

Ensure you have installed:
- **Docker** and **Docker Compose** (for infrastructure)
- **Java 17** (for backend services)
- **Maven** (for building Java services)
- **Node.js 18+** (for Angular frontend)
- **npm** (Node package manager)

## 🚀 Quick Start (One Command)

Run everything in one shot:

```bash
# Terminal 1: Start Docker infrastructure (Kafka, PostgreSQL)
docker-compose up -d

# Terminal 2: Build and start backend services
mvn clean install
mvn spring-boot:run

# Terminal 3: Start Angular frontend
cd frontend-angular
npm install
npm start
```

## 📖 Step-by-Step Instructions

### Step 1: Start Docker Infrastructure

Docker Compose will start:
- **Zookeeper** (for Kafka coordination)
- **Kafka** broker (event streaming)
- **Kafka Init** (creates topics automatically)
- **4 PostgreSQL** databases (one per microservice)

```bash
# Start all containers
docker-compose up -d

# Verify everything is running
docker-compose ps

# View logs
docker-compose logs -f
```

**What gets created:**
- ✅ `industry-zookeeper` on port 2181
- ✅ `industry-kafka` on ports 9092, 29092
- ✅ `industry-postgres-raw-material` on port 5431
- ✅ `industry-postgres-processing` on port 5432
- ✅ `industry-postgres-component` on port 5433
- ✅ `industry-postgres-assembly` on port 5434

### Step 2: Initialize Java Backend

Build and prepare all microservices:

```bash
# Build entire project
mvn clean package -DskipTests

# Or build specific services
mvn clean package -pl common-models,raw-material-service -am
```

### Step 3: Start Backend Services (Option A: Individually)

Start each microservice in a separate terminal:

```bash
# Terminal 2A: Raw Material Service
cd raw-material-service
mvn spring-boot:run
# Runs on http://localhost:8081

# Terminal 2B: Processing Service
cd processing-service
mvn spring-boot:run
# Runs on http://localhost:8082

# Terminal 2C: Component Service
cd component-service
mvn spring-boot:run
# Runs on http://localhost:8083

# Terminal 2D: Assembly Service
cd assembly-service
mvn spring-boot:run
# Runs on http://localhost:8084
```

### Step 3: Start Backend Services (Option B: Docker)

Use Docker Compose to run services with Docker:

```bash
# After building with Maven:
docker-compose up raw-material-service processing-service component-service assembly-service

# View individual service logs
docker-compose logs -f raw-material-service
```

### Step 4: Start Angular Frontend

In a new terminal:

```bash
cd frontend-angular

# Install dependencies (first time only)
npm install

# Start development server
npm start
```

The application will automatically open at: **http://localhost:4200** 🎉

## ✅ Verification Checklist

Confirm everything is running:

```bash
# Check backend services are accessible
curl http://localhost:8081/api/raw-materials/health
curl http://localhost:8082/api/processing/health
curl http://localhost:8083/api/components/health
curl http://localhost:8084/api/assembly/health

# Check Kafka is running
docker exec industry-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check PostgreSQL databases
psql -h localhost -p 5431 -U industry_user -d raw_material_db -c "SELECT version();"
```

## 🎮 Usage Walkthrough

### 1. Configure Pipeline
1. Open **http://localhost:4200** in your browser
2. Click **Configuration** tab
3. Click **⚙️ Pipeline Configuration**
4. Enter pipeline name and add production steps
5. Click **Save Pipeline**

### 2. Create Market Order
1. In the **Configuration** tab, go to **📝 New Production Order**
2. Fill in customer information
3. Select product type (SEDAN, SUV, TRUCK, SPORTS)
4. Enter quantity and BOM version
5. Set delivery date and priority
6. Click **Create Order**

### 3. Monitor Production
1. Click **Monitoring** tab
2. View **📡 Events Monitor** (Kafka events)
3. View **📦 Inventory Monitor** (stock levels)

### 4. Check Statistics
1. Click **Statistics** tab
2. View order metrics and performance indicators

## 📊 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Angular 17 Frontend (4200)                  │
│                    Pipeline Config • Orders • Monitor            │
└──────────────────────┬──────────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   HTTP REST        HTTP REST    HTTP REST
        │              │              │
    ┌───▼──────┐  ┌────▼────┐  ┌────▼────┐  ┌────────────┐
    │ 8081     │  │ 8082    │  │ 8083    │  │ 8084       │
    │Raw       │  │Process  │  │Comp     │  │Assembly    │
    │Material  │  │Service  │  │Service  │  │Service     │
    └────┬─────┘  └────┬────┘  └────┬────┘  └────┬───────┘
         │             │            │             │
         └─────────────┼────────────┼─────────────┘
                       │
                   KAFKA Events
                 (5 Topics)
                       │
         ┌─────────────┴─────────────┐
         │                           │
    PostgreSQL (5431)         PostgreSQL (5432)
    raw_material_db          processing_db
         │                           │
    PostgreSQL (5433)         PostgreSQL (5434)
    component_db               assembly_db
```

## 🐛 Troubleshooting

### Ports Already in Use
```bash
# Kill process on specific port
# Linux/Mac:
lsof -ti:4200 | xargs kill -9

# Windows:
netstat -ano | findstr :4200
taskkill /PID <PID> /F
```

### Docker Container Errors
```bash
# Remove all containers and restart
docker-compose down -v
docker-compose up -d
```

### Backend Service Won't Start
```bash
# Check logs for errors
docker-compose logs raw-material-service

# Verify database connectivity
docker exec industry-postgres-raw-material psql -U industry_user -d raw_material_db -c "SELECT 1;"
```

### Frontend Connection Errors
1. Verify backend services are running (check port 8081-8084)
2. Check browser console for CORS errors
3. Ensure backend CORS configuration allows localhost:4200

### Kafka Topics Not Created
```bash
# Manually create topics
docker exec industry-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic raw-material-produced --if-not-exists
```

## 🧹 Cleanup

Stop and remove all containers:

```bash
# Stop all services
docker-compose down

# Remove volumes (delete data)
docker-compose down -v

# Clean build artifacts
mvn clean
```

## 📁 File Structure

```
IndustrySimulator/
├── docker-compose.yml          # Infrastructure config
├── init-databases.sql          # Database initialization
├── init-kafka-topics.sh        # Kafka topics setup
├── pom.xml                     # Maven parent POM
│
├── common-models/              # Shared models
├── raw-material-service/       # Service 1 (8081)
├── processing-service/         # Service 2 (8082)
├── component-service/          # Service 3 (8083)
├── assembly-service/           # Service 4 (8084)
│
└── frontend-angular/           # Angular dashboard (4200)
    ├── package.json
    ├── src/
    │   ├── app/
    │   │   ├── components/     # Dashboard components
    │   │   ├── services/       # API & Event services
    │   │   ├── models/         # TypeScript interfaces
    │   │   └── app.component.ts
    │   └── main.ts
    └── README.md
```

## 🔧 Advanced Usage

### Run Full Build with Tests
```bash
mvn clean verify
```

### Build Frontend Production Bundle
```bash
cd frontend-angular
npm run build:prod
```

### Deploy Frontend
```bash
# Serve optimized build
npx http-server frontend-angular/dist/industry-simulator/
```

### View Real-Time Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f raw-material-service
```

### Execute Database Query
```bash
# Connect to specific database
docker exec -it industry-postgres-raw-material psql \
  -U industry_user \
  -d raw_material_db \
  -c "SELECT * FROM raw_material;"
```

### Monitor Kafka Topics
```bash
# List all topics
docker exec industry-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Monitor specific topic
docker exec industry-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic raw-material-produced \
  --from-beginning
```

## 📞 Support

For issues or questions:
1. Check service logs: `docker-compose logs SERVICE_NAME`
2. Verify port connectivity: `netstat -an | grep LISTEN`
3. Review backend application.yml files
4. Check frontend browser console (F12)

## ✨ Features Ready to Use

✅ Pipeline configuration with real-time duration calculation
✅ Market order creation with priority levels
✅ Real-time Kafka event monitoring
✅ Live inventory tracking with stock alerts
✅ Production statistics and metrics
✅ Multi-tab dashboard interface
✅ Responsive design (works on mobile)
✅ Help documentation built-in

## 🎯 Next Steps

1. **Configure your first pipeline** - Set up production steps
2. **Create test orders** - Submit market orders for production
3. **Monitor events** - Watch Kafka events in real-time
4. **Check inventory** - Track stock levels
5. **Review statistics** - Analyze performance metrics

Happy Production! 🚀

---

**Project Structure**: Monorepo Maven + Docker + Angular 17
**Technologies**: Spring Boot 3.2, Kafka 7.5, PostgreSQL 16, Angular 17
**Total Microservices**: 4 (+1 frontend)
**Default Ports**: 4200 (Frontend), 8081-8084 (Services), 9092 (Kafka), 5431-5434 (PostgreSQL)
