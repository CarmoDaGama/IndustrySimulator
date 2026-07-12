# Simulador da Indústria 4.0

**Universidade Católica de Angola** — Faculdade de Engenharia
Engenharia Informática · Sistemas Distribuídos e Paralelos II · 4.º ano (2026/2027)

Simulador de uma cadeia industrial vertical — da extracção de matéria-prima até à venda ao
cliente — construído sobre microserviços distribuídos, comunicação assíncrona por Kafka e
paralelismo real baseado em Threads.

---

## Índice

1. [Arranque rápido](#1-arranque-rápido)
2. [Arquitectura](#2-arquitectura)
3. [Conceitos centrais](#3-conceitos-centrais-o-que-defender)
4. [Portal de configurações](#4-portal-de-configurações)
5. [Como demonstrar cada requisito](#5-como-demonstrar-cada-requisito-guião-de-defesa)
6. [Referência de API](#6-referência-de-api)
7. [Modelo de dados e eventos](#7-modelo-de-dados-e-eventos)
8. [Resolução de problemas](#8-resolução-de-problemas)

---

## 1. Arranque rápido

### Pré-requisitos

| Ferramenta | Versão | Notas |
|---|---|---|
| JDK | 17+ | O projecto compila para Java 17 |
| Maven | 3.9+ | |
| Docker | qualquer | **Só** para Kafka e PostgreSQL |
| Node.js | 18+ | Para o portal Angular |

### Passos

```bash
# 1. Compilar tudo (gera os 4 JARs)
mvn clean package -DskipTests

# 2. Subir APENAS a infraestrutura (Kafka + 1 servidor PostgreSQL)
docker compose up -d

# 3. Correr os microserviços NATIVAMENTE (4 terminais, um por serviço)
java -jar raw-material-service/target/raw-material-service-1.0.0-SNAPSHOT.jar   # :8081
java -jar processing-service/target/processing-service-1.0.0-SNAPSHOT.jar       # :8082
java -jar component-service/target/component-service-1.0.0-SNAPSHOT.jar         # :8083
java -jar assembly-service/target/assembly-service-1.0.0-SNAPSHOT.jar           # :8084

# 4. Portal de configurações
cd frontend-angular && npm install && npm start   # http://localhost:4200
```

**Login do portal:** `admin` / `admin123`

> **A cadeia arranca vazia por desenho.** A Camada 1 só extrai depois de configurada.
> Vá a **Configurações → Recursos Extraídos** e adicione um recurso (ex.: Ferro), ou:
>
> ```bash
> curl -X POST http://localhost:8081/api/raw-materials/extraction-config \
>   -H "Content-Type: application/json" \
>   -d '{"materialName":"Ferro","materialType":"steel","quantityPerCycle":1,"unit":"kg",
>        "extractionDurationMs":3000,"transportDurationMs":1000,"factory":"mina-luanda",
>        "targetProduct":"CAR","targetComponent":"ENGINE","description":"Aço para motor","active":true}'
> ```

---

## 2. Arquitectura

### Cadeia de valor

```
[C1 Extracção] → [C2 Processamento] → [C3 Componentes] → [C4 Montagem] → [C5 Inventário] → [C6 Mercado]
     :8081             :8082                :8083              :8084 ────────────┴──────────────┘
```

As Camadas 5 (Inventário) e 6 (Mercado) vivem dentro do `assembly-service`, cada uma com o seu
próprio domínio e tabelas.

### Comunicação

- **Assíncrona (Kafka)** — todo o fluxo de produção entre camadas. Tópicos:
  `raw-material-produced` → `processing-completed` → `component-assembled` → `product-assembled` + `inventory-updated`
- **Síncrona (REST)** — exclusivamente para monitorização, logs e leitura/escrita de configurações.

### Persistência

**Um único servidor PostgreSQL** (`industry-postgres`), com **uma base de dados isolada por
microserviço**. Nenhum serviço acede à base de dados de outro.

| Serviço | Base de dados | Tabelas |
|---|---|---|
| raw-material | `raw_material_db` | raw_materials, extraction_config, worker_pool_config |
| processing | `processing_db` | processed_materials, pipeline_step, worker_pool_config |
| component | `component_db` | components, pipeline_step, compatible_material, worker_pool_config |
| assembly | `assembly_db` | products, inventory, market_order, pipeline_step, worker_pool_config |

### Restrição de Docker

Docker corre **apenas** o Kafka e o servidor de base de dados. Os microserviços correm
nativamente (`java -jar`) — **não existem Dockerfiles** para eles, por exigência do enunciado.

### Estrutura do repositório

```
common-models/            Modelos e eventos partilhados + os pools de Workers
  └─ worker/              TwoToOneWorkerPool, ContinuousWorkerPool, StepSpec, WorkerActivity
raw-material-service/     Camada 1 — extracção autónoma
processing-service/       Camada 2 — refinação
component-service/        Camada 3 — peças + validação BOM
assembly-service/         Camadas 4, 5 e 6 — montagem, inventário e mercado
frontend-angular/         Portal de configurações e monitorização
docker-compose.yml        Kafka + Zookeeper + PostgreSQL (só infraestrutura)
```

---

## 3. Conceitos centrais (o que defender)

### 3.1 Workers baseados em Threads

Cada microserviço tem uma **pool de Workers**, e cada Worker **é uma Thread real** que
representa uma linha de produção activa. Não são `CompletableFuture` nem o pool comum da JVM —
são threads dedicadas, com estado próprio e ciclo de vida gerido.

> **Código:** `common-models/src/main/java/com/industry/simulator/common/worker/TwoToOneWorkerPool.java`
> — classe interna `Worker extends Thread`.

O número de Workers é **parametrizável em runtime** pelo portal (`PUT /workers`), sem reiniciar
o serviço: `resize()` cria ou termina threads conforme necessário.

### 3.2 Regra de consumo 2:1

Excepto nas Camadas 1 e 6, **cada unidade produzida consome 2 unidades da camada anterior**.
Está implementado no ciclo do Worker, que só avança depois de retirar 2 itens da fila:

```java
for (int i = 0; i < inputsPerOutput; i++) {   // inputsPerOutput = 2
    IN item = inputQueue.take();              // bloqueia se não houver
    batch.add(item);
}
```

**Consequência observável:** para sair 1 produto final são precisas 8 matérias-primas
(8 → 4 → 2 → 1). É por isso que uma única matéria-prima "não faz nada" — está correcto.

### 3.3 Bloqueio por escassez

`BlockingQueue.take()` bloqueia a Thread quando a camada anterior esgota. O Worker não faz
*polling* nem consome CPU: fica suspenso até chegar um novo evento de reabastecimento.
No portal, esses Workers aparecem a **laranja (BLOCKED)**.

### 3.4 Pipeline com tempo, etapa a etapa

Cada Worker percorre as etapas configuradas **uma a uma**, cronometrando cada uma e publicando
o seu estado. É isto que permite ver, em tempo real, *qual* etapa está em execução — e não
apenas o resultado final.

```java
for (StepSpec step : stepsSupplier.get()) {   // etapas vindas da BD
    state = RUNNING; currentStep = step.getName();
    Thread.sleep(step.getDurationMs());
}
```

### 3.5 Genericidade (nada hardcoded)

Todos os nomes, tempos e regras vivem em **base de dados**, editáveis pelo portal:

| O quê | Tabela |
|---|---|
| Recursos a extrair, tempos e `purpose` | `extraction_config` |
| Etapas e durações da pipeline | `pipeline_step` (uma por serviço) |
| Nº de Workers | `worker_pool_config` |
| Regras de compatibilidade BOM | `compatible_material` |

---

## 4. Portal de configurações

`http://localhost:4200` — autenticação obrigatória (`admin` / `admin123`).

| Separador | Conteúdo |
|---|---|
| **Operações** | Pipeline por microserviço (etapas + `durationMs`), criação de encomendas |
| **Configurações** | Nº de Workers por microserviço, recursos extraídos (Camada 1), regras BOM |
| **Live Monitor** | **Processos em execução** (etapa actual de cada Worker), eventos Kafka, inventário |

### Monitor de processos

Mostra as 4 camadas em cadeia. Para cada Worker: a **etapa em execução**, uma **barra de
progresso** da etapa, o **lote** a ser processado e a **fila** de cada camada.

- 🟢 **RUNNING** — a executar uma etapa
- 🟠 **BLOCKED** — à espera de insumos (bloqueio por escassez)
- ⚪ **IDLE** — sem configuração activa

Uma fila que cresce indica um **gargalo** — aumente os Workers dessa camada e observe o efeito.

---

## 5. Como demonstrar cada requisito (guião de defesa)

### Um só servidor de BD, com bases isoladas

```bash
docker compose ps                                              # um só postgres
docker exec industry-postgres psql -U industry_user -c "\l"    # 4 bases

# Prova ao vivo: cada base tem exactamente um pool de ligações (um serviço)
docker exec industry-postgres psql -U industry_user -c \
  "SELECT datname, count(*) FROM pg_stat_activity WHERE datname LIKE '%_db' GROUP BY datname;"

# Cada base só tem as tabelas da sua camada
docker exec industry-postgres psql -U industry_user -d component_db -c "\dt"
```

### Microserviços fora do Docker

```bash
ls */Dockerfile           # não existe nenhum
docker compose ps         # só kafka, zookeeper e postgres
```

### Concorrência real em Threads + etapas em execução

```bash
curl http://localhost:8082/api/processing/workers/activity
```

Mostra cada Thread, o seu estado e a etapa actual — ou abra **Live Monitor** no portal.

### Regra 2:1

Deixe a cadeia correr e compare as contagens: cada camada produz ~metade da anterior.

```bash
curl -s http://localhost:8081/api/raw-materials | grep -o '"batchId"' | wc -l
curl -s http://localhost:8082/api/processing    | grep -o '"batchId"' | wc -l
curl -s http://localhost:8083/api/components    | grep -o '"batchId"' | wc -l
curl -s http://localhost:8084/api/inventory
```

### Bloqueio por escassez

Ponha a Camada 1 com tempos longos (ou 0 Workers) e observe as camadas seguintes a ficarem
**BLOCKED** no monitor — sem consumir CPU.

### Workers parametrizáveis

```bash
curl -X PUT http://localhost:8082/api/processing/workers \
  -H "Content-Type: application/json" -d '{"workerCount":4}'
```

O log mostra `Pool redimensionado para 4 worker(s)` e a fila escoa mais depressa.

### Pedido PENDENTE desbloqueado ao repor stock (Camada 6)

Crie uma encomenda com quantidade acima do stock — fica `PENDING`. Quando a produção repõe
inventário, o consumidor de `inventory-updated` aloca-a automaticamente:

```
Pedido <id> desbloqueado (era PENDING) após reposição de stock
```

### Contrato de dados (`purpose`, `producer`, `components`)

```bash
docker exec industry-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 --topic component-assembled --from-beginning --max-messages 1
```

O payload traz `purpose` (targetProduct/targetComponent/description), `producer.service`,
`producer.factory` e a árvore `components[]` dos nós consumidos.

---

## 6. Referência de API

REST é usado **apenas** para configuração e monitorização — nunca para o fluxo de produção.

### Comum a todos os serviços

Substitua `{base}` por `raw-materials` (8081), `processing` (8082), `components` (8083) ou
`assembly` (8084).

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/{base}/workers` | Nº de Workers e tamanho da fila |
| `PUT` | `/api/{base}/workers` | Alterar o nº de Workers — `{"workerCount": 4}` |
| `GET` | `/api/{base}/workers/activity` | **Etapa em execução de cada Worker** |
| `GET` | `/api/{base}/pipeline` | Etapas configuradas (excepto raw-materials) |
| `POST` | `/api/{base}/pipeline` | Substituir as etapas |

### Específicos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET/POST/PUT/DELETE` | `:8081/api/raw-materials/extraction-config` | Recursos extraídos pela Camada 1 |
| `GET/POST/DELETE` | `:8083/api/components/bom/compatible-materials` | Regras de compatibilidade BOM |
| `GET` | `:8084/api/inventory` | Stock global (Camada 5) |
| `GET/POST` | `:8084/api/market-orders` | Encomendas (Camada 6) |
| `GET` | `:8084/api/market-orders/status/{status}` | Filtrar por `PENDING` / `ALLOCATED` |

> **Encomendas** exigem `bomVersion` no formato `vX.Y.Z` e `requiredDeliveryDate`.

---

## 7. Modelo de dados e eventos

Todo o item da cadeia que circula em Kafka respeita o contrato obrigatório:

```json
{
  "eventId": "uuid-v4",
  "eventType": "COMPONENT_CREATED",
  "timestamp": 1710000000,
  "payload": {
    "id": "comp-7781",
    "name": "Processador",
    "type": "COMPONENT",
    "purpose": {
      "targetProduct": "SMARTPHONE",
      "targetComponent": "MOTHERBOARD",
      "description": "Circuito integrado de processamento central"
    },
    "producer": { "service": "component-service", "factory": "manufacturing-hub-gamma" },
    "components": []
  }
}
```

`components[]` é preenchido com as **2 unidades** consumidas, formando a árvore de dependências
recursiva até à matéria-prima.

---

## 8. Resolução de problemas

**A cadeia não produz nada.**
Falta configurar um recurso de extracção (ver [Arranque rápido](#1-arranque-rápido)). Sem isso,
os Workers da Camada 1 ficam `IDLE` e nada entra na cadeia.

**Só saiu 1 produto e eu criei 8 matérias-primas.**
Correcto — é a regra 2:1 (8 → 4 → 2 → 1).

**A porta 5432 já está ocupada.**
Se outro PostgreSQL local usar a 5432, crie um `docker-compose.override.yml` (não versionado):

```yaml
services:
  postgres:
    ports: !override
      - "5442:5432"
```

E arranque cada serviço apontando para a nova porta:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5442/processing_db \
  java -jar processing-service/target/processing-service-1.0.0-SNAPSHOT.jar
```

**Os tópicos Kafka não existem.**

```bash
docker compose up kafka-init
docker exec industry-kafka kafka-topics --bootstrap-server localhost:29092 --list
```

**Endpoints com `{id}` devolvem HTTP 400.**
O projecto compila com `-parameters` (exigido pelo Spring Boot 3.2+). Recompile com
`mvn clean package`.
