# Simulador Industrial - Arquitetura de Microserviços

Um sistema distribuído simulando uma cadeia de suprimentos industrial usando arquitetura de microserviços com Spring Boot, Apache Kafka e PostgreSQL.

## 📑 Índice

- [Estrutura do Projeto](#estrutura-do-projeto)
- [Visão Geral da Arquitetura](#visão-geral-da-arquitetura)
- [Começando](#começando)
  - [Pré-requisitos](#pré-requisitos)
  - [Instalação de Pré-requisitos](#instalação-de-pré-requisitos)
  - [Início Rápido com Docker](#início-rápido-com-docker)
  - [Desenvolvimento Local (sem Docker)](#desenvolvimento-local-sem-docker)
- [Endpoints da API](#endpoints-da-api)
- [Desenvolvimento](#desenvolvimento)
- [Decisões de Design](#decisões-de-design)
- [Resolução de Problemas](#resolução-de-problemas)
- [Próximos Passos](#próximos-passos)

## Estrutura do Projeto

```
├── common-models/              # DTOs, Eventos e Modelos Compartilhados
│   └── src/main/java/com/industry/simulator/common/
│       ├── dto/               # Objetos de Transferência de Dados
│       ├── events/            # Definições de Eventos Kafka
│       └── model/             # Modelos de Domínio (Componente, BOM, Passo de Pipeline)
│
├── raw-material-service/      # Serviço 1: Produção de Matéria-Prima
│   ├── src/main/java/...      # Controller, Serviço, Repositório, Produtor Kafka
│   ├── src/main/resources/    # configuração application.yml
│   └── Dockerfile
│
├── processing-service/        # Serviço 2: Processamento de Material
├── component-service/         # Serviço 3: Montagem de Componentes
├── assembly-service/          # Serviço 4: Montagem de Produtos & Inventário & Mercado
│
├── frontend-angular/          # Frontend Angular 17
│   ├── src/                   # Código-fonte Angular
│   ├── package.json           # Dependências npm
│   └── angular.json           # Configuração Angular
│
├── docker-compose.yml         # Orquestração: Zookeeper, Kafka, PostgreSQL, Serviços
├── pom.xml                    # POM pai do Maven
└── README.md
```

## Visão Geral da Arquitetura

### Microserviços

1. **raw-material-service** (Porta 8081)
   - Produz matérias-primas
   - Publica `RawMaterialProducedEvent` no Kafka
   - Banco de Dados: `raw_material_db`

2. **processing-service** (Porta 8082)
   - Processa matérias-primas (derretimento, corte, etc.)
   - Consome `RawMaterialProducedEvent`
   - Publica `ProcessingCompletedEvent`
   - Banco de Dados: `processing_db`

3. **component-service** (Porta 8083)
   - Monta componentes a partir de materiais processados
   - Consome `ProcessingCompletedEvent`
   - Publica `ComponentAssembledEvent`
   - Banco de Dados: `component_db`

4. **assembly-service** (Porta 8084)
   - Montagem de produto final
   - Gerencia inventário e pedidos de mercado
   - Consome eventos de componentes
   - Publica `ProductAssembledEvent` e `InventoryUpdatedEvent`
   - Banco de Dados: `assembly_db`

5. **frontend-angular** (Porta 4200)
   - Interface de usuário Angular 17
   - Dashboard para monitoramento e controle
   - Comunicação WebSocket com os serviços

### Tópicos Kafka

- `raw-material-produced` - Matérias-primas prontas para processamento
- `processing-completed` - Materiais processados prontos para montagem
- `component-assembled` - Componentes prontos para montagem final
- `product-assembled` - Produtos finais prontos para envio
- `inventory-updated` - Mudanças de inventário (entrada/saída de estoque)

## Começando

### Pré-requisitos

- **Docker & Docker Compose**
- **Java 17+** (para compilação local do Backend)
- **Maven 3.9+** (para build do Backend)
- **Node.js 18+** (para o Frontend Angular)
- **npm 9+** (para gerenciar dependências do Frontend)
- **Git** (para controle de versão)

#### Instalação de Pré-requisitos

##### 1. Docker e Docker Compose

**Linux (Ubuntu/Debian):**
```bash
# Instalar Docker
sudo apt-get update
sudo apt-get install docker.io -y
sudo usermod -aG docker $USER
newgrp docker

# Instalar Docker Compose
sudo apt-get install docker-compose -y

# Verificar instalação
docker --version
docker-compose --version
```

**macOS:**
```bash
# Instalar via Homebrew
brew install docker docker-compose

# Ou baixar Docker Desktop: https://www.docker.com/products/docker-desktop
```

**Windows:**
- Baixar Docker Desktop: https://www.docker.com/products/docker-desktop
- Seguir o assistente de instalação

##### 2. Java 17+

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install openjdk-17-jdk -y
java -version
```

**macOS:**
```bash
brew install openjdk@17
java -version
```

**Windows:**
- Baixar em: https://www.oracle.com/java/technologies/downloads/
- Seguir o assistente de instalação
- Adicionar JAVA_HOME às variáveis de ambiente

##### 3. Maven 3.9+

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install maven -y
mvn --version
```

**macOS:**
```bash
brew install maven
mvn --version
```

**Windows:**
- Baixar em: https://maven.apache.org/download.cgi
- Extrair o arquivo
- Adicionar pasta `bin` ao PATH

##### 4. Node.js 18+ e npm 9+

**Linux (Ubuntu/Debian):**
```bash
# Instalar Node.js e npm
sudo apt-get install nodejs npm -y

# Verificar versão
node --version
npm --version

# Se versão estiver antiga, atualizar com nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18
```

**macOS:**
```bash
# Via Homebrew
brew install node@18

# Via nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18
```

**Windows:**
- Baixar em: https://nodejs.org/
- Escolher versão LTS (18+)
- Seguir o assistente de instalação

### Configuração: Docker vs Desenvolvimento Local

#### 🐳 Em Docker (Recomendado)

```
Raw Material Service:  localhost:8081
Processing Service:    localhost:8082
Component Service:     localhost:8083
Assembly Service:      localhost:8084
Frontend Angular:      localhost:4200
Kafka (Producer):      kafka:29092 (interno ao container)
Kafka (Cliente externo): localhost:9092
Zookeeper:             localhost:2181

Banco de Dados:
- Raw Material:  postgres-raw-material:5432 (porta interna) / localhost:5431 (acesso externo)
- Processing:    postgres-processing:5432 (porta interna) / localhost:5435 (acesso externo)
- Component:     postgres-component:5432 (porta interna) / localhost:5433 (acesso externo)
- Assembly:      postgres-assembly:5432 (porta interna) / localhost:5434 (acesso externo)

Usuário/Senha:  industry_user / industry_password
```

#### 💻 Em Desenvolvimento Local (sem Docker)

```
Raw Material Service:  localhost:8081
Processing Service:    localhost:8082
Component Service:     localhost:8083
Assembly Service:      localhost:8084
Frontend Angular:      localhost:4200
Kafka:                 localhost:9092
Zookeeper:             localhost:2181

Banco de Dados (todos na mesma máquina):
- Todos:      localhost:5432 (porta padrão PostgreSQL)
- Databases:  raw_material_db, processing_db, component_db, assembly_db

Usuário/Senha:  industry_user / industry_password
```

### Início Rápido com Docker

Esta é a forma **mais fácil** e recomendada para executar o projeto.

#### Passo 1: Compilar o Backend

```bash
# Navegar para a raiz do projeto
cd /caminho/para/IndustrySimulator

# Compilar todos os serviços
mvn clean package -DskipTests
```

#### Passo 2: Iniciar os Serviços (Backend)

```bash
# Iniciar todos os serviços com Docker Compose
docker-compose up --build

# Ou em modo detached (background)
docker-compose up -d --build
```

Aguarde aproximadamente **30-40 segundos** para que todos os serviços sejam inicializados.

#### Passo 3: Verificar Saúde dos Serviços

```bash
# Serviço de Matéria-Prima
curl http://localhost:8081/api/raw-materials/health

# Serviço de Processamento
curl http://localhost:8082/api/processing/health

# Serviço de Componentes
curl http://localhost:8083/api/components/health

# Serviço de Montagem (Inventário)
curl http://localhost:8084/api/inventory/health

# Ou Serviço de Montagem (Pedidos de Mercado)
curl http://localhost:8084/api/market-orders/health
```

Todos devem retornar status de saúde positivo.

#### Passo 4: Executar o Frontend

Em outro terminal, navegue até a pasta do frontend:

```bash
cd frontend-angular

# Instalar dependências
npm install

# Iniciar o servidor de desenvolvimento (abre no navegador automaticamente)
npm start

# Ou iniciar sem abrir o navegador
npm run dev
```

O frontend estará disponível em: **http://localhost:4200**

#### Passo 5: Testar o Sistema

Agora você pode:
- Acessar a interface em http://localhost:4200
- Fazer requisições para os serviços (ver seção de Endpoints da API)
- Monitorar os eventos no Kafka

### Parar os Serviços

```bash
# Parar todos os containers Docker
docker-compose down

# Ou parar mantendo os volumes (dados preservados)
docker-compose down -v
```

### Inicializar Tópicos Kafka (Opcional)

Os tópicos são criados automaticamente, mas você pode criá-los manualmente:

```bash
chmod +x init-kafka-topics.sh
./init-kafka-topics.sh
```

## Endpoints da API

### Serviço de Matéria-Prima (Porta 8081)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/raw-materials` | Criar nova matéria-prima |
| GET | `/api/raw-materials` | Listar todos os materiais |
| GET | `/api/raw-materials/batch/{batchId}` | Obter materiais por lote |
| GET | `/api/raw-materials/type/{materialType}` | Obter materiais por tipo |
| GET | `/api/raw-materials/health` | Verificar saúde do serviço |

### Serviço de Processamento (Porta 8082)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/processing/health` | Verificar saúde do serviço |
| GET | `/api/processing/pipeline` | Informações do pipeline |

### Serviço de Componentes (Porta 8083)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/components/health` | Verificar saúde do serviço |
| GET | `/api/components` | Listar componentes |

### Serviço de Montagem (Porta 8084)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/inventory/health` | Verificar saúde (inventário) |
| GET | `/api/market-orders/health` | Verificar saúde (pedidos) |
| GET | `/api/inventory` | Listar inventário |
| POST | `/api/market-orders` | Criar novo pedido de mercado |
| GET | `/api/market-orders` | Listar pedidos de mercado |
| GET | `/api/orders/ws` | WebSocket para atualizações em tempo real |

### Exemplo de Requisição

Criar uma nova matéria-prima:

```bash
curl -X POST http://localhost:8081/api/raw-materials \
  -H "Content-Type: application/json" \
  -d '{
    "materialName": "Minério de Ferro",
    "materialType": "ORE",
    "quantity": 100.0,
    "unit": "kg"
  }'
```

Listar todas as matérias-primas:

```bash
curl http://localhost:8081/api/raw-materials
```

### Testando WebSocket (Atualizações em Tempo Real)

O projeto inclui um script de teste WebSocket para simular atualizações em tempo real:

```bash
# Tornar script executável
chmod +x test-websocket.sh

# Executar teste (cria um pedido e simula transições de status)
./test-websocket.sh
```

Este script:
1. Cria um novo pedido de mercado
2. Produz uma matéria-prima raw
3. Simula transições de status do pedido
4. Envia atualizações via WebSocket aos clientes conectados

Você pode monitorar estas mudanças no dashboard Angular em tempo real.

## Desenvolvimento

### Variáveis de Ambiente (Development Local)

Para facilitar o desenvolvimento local, você pode criar um arquivo `.env.local` em cada diretório de serviço:

**raw-material-service/.env.local:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5431/raw_material_db
SPRING_DATASOURCE_USERNAME=industry_user
SPRING_DATASOURCE_PASSWORD=industry_password
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SERVER_PORT=8081
```

Depois use:
```bash
mvn spring-boot:run -pl raw-material-service
```

### Desenvolvimento Local (sem Docker)

#### Para o Backend

1. **Instalar PostgreSQL localmente:**

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install postgresql postgresql-contrib -y
sudo service postgresql start
```

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

**Windows:**
- Baixar em: https://www.postgresql.org/download/windows/
- Seguir o assistente de instalação

2. **Criar os Bancos de Dados:**

```bash
# Conectar ao PostgreSQL como superusuário
psql -U postgres

# Criar usuário industry_user
CREATE USER industry_user WITH PASSWORD 'industry_password';

# Alterar privilégios do usuário
ALTER USER industry_user CREATEDB;

# Criar os bancos de dados
CREATE DATABASE raw_material_db OWNER industry_user;
CREATE DATABASE processing_db OWNER industry_user;
CREATE DATABASE component_db OWNER industry_user;
CREATE DATABASE assembly_db OWNER industry_user;

# Listar bancos criados
\l

# Conceder privilégios necessários
GRANT ALL PRIVILEGES ON DATABASE raw_material_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE processing_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE component_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE assembly_db TO industry_user;

# Sair
\q
```

Ou conectar diretamente com o novo usuário:

```bash
# Conectar como industry_user
psql -U industry_user -d raw_material_db
```

3. **Iniciar Kafka localmente:**

```bash
# Iniciar apenas Zookeeper e Kafka
docker-compose up zookeeper kafka -d
```

⚠️ **Portas do PostgreSQL em Docker:**

Se você estiver rodando os serviços em Docker (mas quer conectar de um cliente externo), as portas são:

```
Raw Material: localhost:5431  → container:5432
Processing:   localhost:5435  → container:5432
Component:    localhost:5433  → container:5432
Assembly:     localhost:5434  → container:5432
```

Se você rodará desenvolvimento local **sem Docker**, use:
```
Todos: localhost:5432 (porta padrão do PostgreSQL)
```

4. **Atualizar Configuração (application.yml)**

Em cada serviço (`raw-material-service`, `processing-service`, `component-service`, `assembly-service`):

Editar `src/main/resources/application.yml` para usar `localhost` em vez de nomes de containers:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/raw_material_db
    # Usar as portas específicas:
    # raw-material-service: 5431
    # component-service: 5433
    # assembly-service: 5434
    # processing-service: 5435
    username: industry_user
    password: industry_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  kafka:
    bootstrap-servers: localhost:9092
```

**Exemplo completo para Raw Material Service:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5431/raw_material_db
    username: industry_user
    password: industry_password
```

5. **Executar Cada Serviço:**

Abra um terminal separado para cada serviço:

```bash
# Terminal 1 - Raw Material Service
mvn spring-boot:run -pl raw-material-service

# Terminal 2 - Processing Service
mvn spring-boot:run -pl processing-service

# Terminal 3 - Component Service
mvn spring-boot:run -pl component-service

# Terminal 4 - Assembly Service
mvn spring-boot:run -pl assembly-service
```

💡 **Dica:** Você pode usar variáveis de ambiente para sobrescrever a configuração:

```bash
# Exemplo com variáveis de ambiente
mvn spring-boot:run -pl raw-material-service \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5431/raw_material_db \
  --spring.kafka.bootstrap-servers=localhost:9092"
```

#### Para o Frontend Angular

```bash
# Navegar para o diretório do frontend
cd frontend-angular

# Instalar dependências (primeira vez ou após atualizar package.json)
npm install

# Iniciar servidor de desenvolvimento
npm start

# Ou sem abrir o navegador automaticamente
npm run dev
```

O frontend estará disponível em: **http://localhost:4200**

### Compilação do Projeto

```bash
# Compilar todos os módulos
mvn clean compile

# Build de todos os módulos (gera JAR)
mvn clean package

# Build de um serviço específico
mvn clean package -pl raw-material-service -am

# Build do frontend (produção)
cd frontend-angular
npm run build:prod
```

### Desenvolvimento do Frontend

```bash
cd frontend-angular

# Instalar dependências
npm install

# Modo desenvolvimento (com hot reload)
npm start

# Build para produção
npm run build:prod

# Executar testes
npm test

# Linting de código
npm run lint
```

## Decisões de Design

1. **Bancos de Dados Independentes**: Cada serviço tem seu próprio banco de dados PostgreSQL para garantir isolamento e independência de dados
2. **Comunicação Assíncrona**: Kafka para comunicação orientada a eventos entre serviços
3. **APIs REST Síncronas**: Para endpoints de gerenciamento e monitoramento
4. **Simulação de Pipeline**: Usa `CompletableFuture.delayedExecutor` ou `Thread.sleep` para simular tempo real de processamento
5. **Versionamento de Eventos**: Todos os eventos incluem um campo `purpose` (requisito v2) para roteamento e monitoramento flexível

## Formato de Mensagem Kafka

### RawMaterialProducedEvent
```json
{
  "eventId": "uuid",
  "batchId": "uuid",
  "material": {
    "id": "uuid",
    "name": "Minério de Ferro",
    "type": "RAW_MATERIAL",
    "quantity": 100.0,
    "unit": "kg"
  },
  "timestamp": "2026-04-28T12:00:00",
  "purpose": "processing",
  "sourceService": "raw-material-service"
}
```

## Resolução de Problemas

### ✅ Checklist de Verificação

Antes de abrir uma issue, verifique:

- [ ] Todos os pré-requisitos estão instalados? `java -version`, `mvn -version`, `node -version`, `docker -version`
- [ ] Docker Compose está rodando? `docker-compose ps`
- [ ] Todos os containers têm status "healthy"? `docker-compose ps | grep healthy`
- [ ] PostgreSQL está iniciado e acessível?
- [ ] Kafka está rodando? `docker-compose logs kafka | grep started`
- [ ] Portas não estão em uso? `lsof -i :8081` (para cada porta 8081-8084, 4200)
- [ ] Frontend pode acessar os serviços? Abrir DevTools (F12) e verificar Network

### Serviços não iniciam
- Verificar logs do Docker Compose: `docker-compose logs <nome-do-serviço>`
- Garantir que as portas 8081-8084 estão disponíveis: `netstat -an | grep LISTEN`
- Aguardar inicialização do PostgreSQL (primeira execução pode levar 30+ segundos)
- Verificar se volumes Docker foram criados: `docker volume ls`

### Erros de conexão com Kafka
- Verificar se Kafka está rodando: `docker-compose ps kafka`
- Validar que o endereço bootstrap server em `application.yml` corresponde:
  - Docker: `kafka:29092`
  - Local: `localhost:9092`
- Ver logs do Kafka: `docker-compose logs kafka`

### Erros de conexão com Banco de Dados
- Garantir que o container PostgreSQL está rodando: `docker-compose ps postgres-raw-material`
- Verificar que as credenciais correspondem em `docker-compose.yml` e `application.yml`
- Testar conexão manual: `psql -h localhost -p 5431 -U industry_user -d raw_material_db`
- Se não conseguir conectar, pode estar usando portas erradas (ver seção "Configuração: Docker vs Desenvolvimento Local")

### O frontend não conecta aos serviços
- Verificar que todos os serviços estão rodando: `docker-compose ps`
- Garantir que o frontend pode acessar localhost:8081-8084
- Verificar console do navegador (F12 → Console) para mensagens de erro
- Testar requisição manual: `curl http://localhost:8081/api/raw-materials/health`
- Verificar CORS se receber erro de requisição: verificar logs do serviço

### Build falha com erro Maven

```bash
# Limpar cache Maven
mvn clean

# Tentar build novamente
mvn clean package -DskipTests

# Se problema persistir, verificar Java
java -version  # Deve ser 17+
```

### Frontend não inicia

```bash
# Verificar Node/npm
node --version  # Deve ser 18+
npm --version   # Deve ser 9+

# Limpar cache npm
rm -rf node_modules package-lock.json

# Reinstalar dependências
npm install

# Tentar iniciar novamente
npm start
```

## Próximos Passos

1. **Implementar Lógica de Serviço** - Adicionar lógica de negócio a cada serviço
2. **Adicionar Validação de BOM** - Validação e verificação de compatibilidade de componentes
3. **Criar Dashboard Angular** - Frontend para monitoramento e controle
4. **Adicionar Testes Unitários e de Integração** - Cobertura de testes abrangente
5. **Otimização de Performance** - Cache, indexação, processamento assíncrono

## Autores
- Teresa

## Licença

Ver arquivo LICENSE
