# Industry Simulator - Microservices Architecture

A distributed system simulating an industrial supply chain using microservices architecture with Spring Boot, Apache Kafka, and PostgreSQL.

## Project Structure

```
├── common-models/              # Shared DTOs, Events, and Models
│   └── src/main/java/com/industry/simulator/common/
│       ├── dto/               # Data Transfer Objects
│       ├── events/            # Kafka event definitions
│       └── model/             # Domain models (Component, BOM, PipelineStep)
│
├── raw-material-service/      # Service 1: Raw Material Production
│   ├── src/main/java/...      # Controller, Service, Repository, Kafka Producer
│   ├── src/main/resources/    # application.yml configuration
│   └── Dockerfile
│
├── processing-service/        # Service 2: Material Processing
├── component-service/         # Service 3: Component Assembly
├── assembly-service/          # Service 4: Product Assembly & Inventory & Market
│
├── docker-compose.yml         # Orchestration: Zookeeper, Kafka, PostgreSQL, Services
├── pom.xml                    # Maven parent POM
└── README.md
```

## Architecture Overview

### Microservices

1. **raw-material-service** (Port 8081)
   - Produces raw materials
   - Publishes `RawMaterialProducedEvent` to Kafka
   - Database: `raw_material_db`

2. **processing-service** (Port 8082)
   - Processes raw materials (melting, cutting, etc.)
   - Consumes `RawMaterialProducedEvent`
   - Publishes `ProcessingCompletedEvent`
   - Database: `processing_db`

3. **component-service** (Port 8083)
   - Assembles components from processed materials
   - Consumes `ProcessingCompletedEvent`
   - Publishes `ComponentAssembledEvent`
   - Database: `component_db`

4. **assembly-service** (Port 8084)
   - Final product assembly
   - Manages inventory and market orders
   - Consumes component events
   - Publishes `ProductAssembledEvent` and `InventoryUpdatedEvent`
   - Database: `assembly_db`

### Kafka Topics

- `raw-material-produced` - Raw materials ready for processing
- `processing-completed` - Processed materials ready for assembly
- `component-assembled` - Components ready for final assembly
- `product-assembled` - Final products ready for shipment
- `inventory-updated` - Inventory changes (stock in/out)

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+
- Maven 3.9+

### Quick Start

1. **Build the project:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start services with Docker Compose:**
   ```bash
   docker-compose up --build
   ```

3. **Wait for services to start** (approximately 30 seconds)

4. **Initialize Kafka topics** (optional, topics auto-create):
   ```bash
   chmod +x init-kafka-topics.sh
   ./init-kafka-topics.sh
   ```

### Health Check

```bash
# Raw Material Service
curl http://localhost:8081/api/raw-materials/health

# Processing Service
curl http://localhost:8082/health

# Component Service
curl http://localhost:8083/health

# Assembly Service
curl http://localhost:8084/health
```

## API Endpoints

### Raw Material Service

- `POST /api/raw-materials` - Create raw material
- `GET /api/raw-materials` - List all materials
- `GET /api/raw-materials/batch/{batchId}` - Get materials by batch
- `GET /api/raw-materials/type/{materialType}` - Get materials by type

### Example Request

```bash
curl -X POST http://localhost:8081/api/raw-materials \
  -H "Content-Type: application/json" \
  -d '{
    "materialName": "Iron Ore",
    "materialType": "ORE",
    "quantity": 100.0,
    "unit": "kg"
  }'
```

## Development

### Local Development (without Docker)

1. Install PostgreSQL locally and create databases:
   ```sql
   CREATE DATABASE raw_material_db;
   CREATE DATABASE processing_db;
   CREATE DATABASE component_db;
   CREATE DATABASE assembly_db;
   ```

2. Start Kafka locally or in Docker:
   ```bash
   docker-compose up zookeeper kafka
   ```

3. Update `application.yml` in each service to use `localhost` instead of container names

4. Run each service:
   ```bash
   mvn spring-boot:run -pl raw-material-service
   mvn spring-boot:run -pl processing-service
   mvn spring-boot:run -pl component-service
   mvn spring-boot:run -pl assembly-service
   ```

### Project Compilation

```bash
# Compile all modules
mvn clean compile

# Build all modules
mvn clean package

# Build only specific service
mvn clean package -pl raw-material-service -am
```

## Key Design Decisions

1. **Independent Databases**: Each service has its own PostgreSQL database to ensure data isolation and independence
2. **Asynchronous Communication**: Kafka for event-driven communication between services
3. **Synchronous REST APIs**: For management and monitoring endpoints
4. **Pipeline Simulation**: Uses `CompletableFuture.delayedExecutor` or `Thread.sleep` to simulate real processing time
5. **Event Versioning**: All events include a `purpose` field (v2 requirement) for flexible routing and monitoring

## Kafka Message Format

### RawMaterialProducedEvent
```json
{
  "eventId": "uuid",
  "batchId": "uuid",
  "material": {
    "id": "uuid",
    "name": "Iron Ore",
    "type": "RAW_MATERIAL",
    "quantity": 100.0,
    "unit": "kg"
  },
  "timestamp": "2026-04-28T12:00:00",
  "purpose": "processing",
  "sourceService": "raw-material-service"
}
```

## Troubleshooting

### Services won't start
- Check Docker Compose logs: `docker-compose logs <service-name>`
- Ensure ports 8081-8084 are available
- Wait for PostgreSQL to initialize (first startup may take 30+ seconds)

### Kafka connection errors
- Verify Kafka is running: `docker-compose ps kafka`
- Check bootstrap server address in `application.yml` matches container network

### Database connection errors
- Ensure PostgreSQL container is running: `docker-compose ps postgres-db`
- Verify credentials match in `docker-compose.yml` and `application.yml`

## Next Steps

1. **Implement Service Logic** - Add business logic to each service
2. **Add BOM Validation** - Component validation and compatibility checking
3. **Create Angular Dashboard** - Frontend for monitoring and control
4. **Add Unit & Integration Tests** - Comprehensive test coverage
5. **Performance Optimization** - Caching, indexing, async processing

## Authors

- Gama - Backend Architecture & Kafka Implementation
- Teresa - Frontend & API Management

## License

See LICENSE file
