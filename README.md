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

> **A cadeia arranca vazia por desenho** — "a produção inicia assim que existirem
> configurações e matérias-primas disponíveis". São precisas **três** configurações, todas no
> portal:
>
> 1. **Configurações → Recursos Extraídos** — *o quê*: os recursos que a Camada 1 extrai.
> 2. **Configurações → Regras de Produção / BOM** — *como*: o que cada camada consome e produz.
> 3. **Pipelines** — *quanto tempo*: as etapas de cada camada. Sem pipeline não há tempos, e a
>    camada não produz.
>
> Falhando qualquer uma delas, os Workers ficam `IDLE`/`BLOCKED` — é o comportamento correcto,
> não uma avaria.

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
| processing | `processing_db` | processed_materials, production_rule(+_input), pipeline_step, worker_pool_config |
| component | `component_db` | components, production_rule(+_input), compatible_material, pipeline_step, worker_pool_config |
| assembly | `assembly_db` | products, inventory, market_order, production_rule(+_input), customer_simulator_config, pipeline_step, worker_pool_config |

### Restrição de Docker

Docker corre **apenas** o Kafka e o servidor de base de dados. Os microserviços correm
nativamente (`java -jar`) — **não existem Dockerfiles** para eles, por exigência do enunciado.

### Estrutura do repositório

```
common-models/            Modelos e eventos partilhados + os pools de Workers
  └─ worker/              ProductionWorkerPool, ContinuousWorkerPool, ProductionSpec,
                          StepSpec, WorkerActivity
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

> **Código:** `common-models/src/main/java/com/industry/simulator/common/worker/ProductionWorkerPool.java`
> — classe interna `Worker extends Thread`.

O número de Workers é **parametrizável em runtime** pelo portal (`PUT /workers`), sem reiniciar
o serviço: `resize()` cria ou termina threads conforme necessário.

### 3.2 Regra de consumo da cadeia

Excepto nas Camadas 1 e 6, **cada unidade produzida consome no mínimo 2 unidades da camada
anterior**. A quantidade exacta vem da regra configurada, e o portal **recusa** regras que
consumam menos de 2 — a regra do enunciado é validada, não presumida.

Os insumos que chegam são arrumados em **filas por material**. O Worker procura uma regra cujos
insumos estejam *todos* satisfeitos e consome exactamente as quantidades exigidas.

**Consequência observável:** para sair 1 Carro (`Motor ×1 + Pneus ×4`, com cada Motor a exigir
`Aço ×2` e cada Pneu `Borracha ×2`) são precisas dezenas de matérias-primas. É por isso que uma
única matéria-prima "não faz nada" — está correcto.

### 3.3 Bloqueio por escassez

Enquanto nenhuma regra tiver todos os seus insumos disponíveis, o Worker fica suspenso numa
`Condition` — não faz *polling* nem consome CPU. Sem regras configuradas, fica igualmente
bloqueado: "a produção inicia assim que existirem configurações e matérias-primas disponíveis".
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

### 3.5 Mapeamento genérico e árvore BOM

As regras de produção vivem em BD e são editadas no portal. Como uma regra aceita **vários
insumos**, a mesma estrutura cobre os dois requisitos da Secção 6.3:

| Uso | Exemplo |
|---|---|
| Regra de transformação | `Minério de Ferro ×2 → Aço ×1` |
| **Árvore de componentes (BOM)** | `Motor ×1 + Pneus ×4 → Carro ×1` |

O Worker só arranca quando **todos** os insumos de alguma regra estiverem disponíveis; enquanto
não estiverem, bloqueia. É por isso que, com 7 Motores e 7 Pneus, sai **1 Carro** (cada carro
exige 4 pneus) — a BOM é respeitada à letra.

O portal recusa regras que consumam menos de 2 unidades da camada anterior, garantindo a regra
de consumo da cadeia.

### 3.6 Simulação automática de clientes (Camada 6)

Os clientes fictícios são **Threads** que, em ciclo, "pensam" durante um tempo configurável e
encomendam um dos produtos que a fábrica sabe montar (vindos das regras da Camada 4). Se não
houver stock, o pedido fica `PENDENTE` e é desbloqueado automaticamente quando o inventário for
reposto — nenhuma intervenção humana é necessária.

Configura-se em **Configurações → Clientes Fictícios** (nº de clientes, intervalo, quantidades).

### 3.7 Genericidade (nada hardcoded)

Todos os nomes, tempos e regras vivem em **base de dados**, editáveis pelo portal:

| O quê | Tabela |
|---|---|
| Recursos a extrair, tempos e `purpose` | `extraction_config` |
| **Regras de transformação e árvore BOM** | `production_rule` + `production_rule_input` |
| Etapas e durações da pipeline | `pipeline_step` (uma por serviço) |
| Nº de Workers | `worker_pool_config` |
| Clientes fictícios (Camada 6) | `customer_simulator_config` |
| Regras de compatibilidade BOM | `compatible_material` |

---

## 4. Portal de configurações

`http://localhost:4200` — autenticação obrigatória (`admin` / `admin123`).

| Separador | Conteúdo |
|---|---|
| **Operações** | Pipeline por microserviço (etapas + `durationMs`), criação de encomendas |
| **Configurações** | **Regras de Produção / BOM**, nº de Workers, recursos extraídos (Camada 1), clientes fictícios, compatibilidade BOM |
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

### Regra de consumo e árvore BOM

Deixe a cadeia correr e compare as contagens por material: cada camada produz muito menos do que
consome, na proporção definida nas regras.

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
| `GET/POST/DELETE` | `:8082/api/processing/production-rules` | Regras de transformação (Camada 2) |
| `GET/POST/DELETE` | `:8083/api/components/production-rules` | Regras de peças (Camada 3) |
| `GET/POST/DELETE` | `:8084/api/assembly/production-rules` | **Árvore BOM** dos produtos (Camada 4) |
| `GET/PUT` | `:8084/api/market/customers` | Simulação de clientes fictícios |
| `GET` | `:8084/api/market/customers/catalog` | Produtos que a fábrica sabe montar |
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

`components[]` é preenchido com **todas as unidades consumidas** pela regra, formando a árvore de
dependências recursiva até à matéria-prima. Num Carro, por exemplo:

```
Carro
├── Motor   (← Aço ×2 ← Minério de Ferro ×4)
├── Pneu    (← Borracha ×2 ← Areia ×4)
├── Pneu
├── Pneu
└── Pneu
```

Todos os eventos usam o mesmo envelope `{eventId, eventType, timestamp, payload}`.

---

## 8. Resolução de problemas

**`password authentication failed for user "industry_user"`**
Os serviços estão a falar com **outro** PostgreSQL — não com o do simulador. Duas causas comuns,
muitas vezes em conjunto:

1. **Existe um `docker-compose.override.yml` na pasta.** Ele muda o Postgres do simulador para
   outra porta (ex.: 5442) e é específico da máquina onde foi criado. Não está versionado, por
   isso um `git clone` nunca o traz — mas **copiar a pasta** traz. Se não precisa dele, apague-o:
   ```bash
   rm docker-compose.override.yml
   ```
2. **Há um PostgreSQL nativo a ocupar a porta 5432.** É ele que responde e rejeita as
   credenciais (se não houvesse nada à escuta, o erro seria *connection refused*). Verifique e
   pare-o:
   ```bash
   sudo ss -lntp | grep 5432          # quem está na porta?
   sudo systemctl stop postgresql     # se for o Postgres do sistema
   sudo systemctl disable postgresql
   ```

Depois, recrie a infraestrutura e confirme que é o nosso Postgres a responder:

```bash
docker compose down -v     # -v recria a base de dados de raiz
docker compose up -d
docker exec industry-postgres psql -U industry_user -c "\l"   # deve listar as 4 bases
```

> **Prefira `git clone` a copiar a pasta.** A cópia arrasta ficheiros locais (`docker-compose.override.yml`,
> `logs/`, `target/`) que causam exactamente este tipo de problema noutra máquina.

**A cadeia não produz nada.**
Faltam configurações. São precisas **três**, e sem qualquer uma delas os Workers ficam
`IDLE`/`BLOCKED` — que é o comportamento correcto segundo o enunciado:

1. **Recursos extraídos** (Camada 1) — o que a fábrica extrai.
2. **Regras de produção / BOM** (Camadas 2–4) — o que cada camada consome e produz.
3. **Pipelines** (todas as camadas) — os tempos de cada etapa. **Sem pipeline não há tempos, e
   sem tempos não há produção** (produção instantânea é proibida).

**Produziram-se muitos componentes mas poucos produtos finais.**
Correcto — é a BOM a ser respeitada. Se um Carro exige 4 Pneus, 7 Pneus só dão para 1 Carro.
Veja em **Live Monitor** que camada está `BLOCKED` e que fila está a crescer: é o gargalo.

**Os clientes não encomendam nada.**
A simulação arranca a 0 clientes. Ligue-a em **Configurações → Clientes Fictícios**. Se o
catálogo estiver vazio, configure primeiro as regras de produção da Camada 4.

**A porta 5432 já está ocupada e quero manter o outro PostgreSQL a correr.**
A alternativa a pará-lo é expor o Postgres do simulador noutra porta. Crie um
`docker-compose.override.yml` — **local, nunca versionado, e que não deve ser copiado para outra
máquina** (é a causa do erro de autenticação descrito acima):

```yaml
services:
  postgres:
    ports: !override
      - "5442:5432"
```

Nesse caso, **todos** os serviços têm de ser arrancados a apontar para a nova porta, senão vão
ligar-se ao PostgreSQL errado:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5442/processing_db \
  java -jar processing-service/target/processing-service-1.0.0-SNAPSHOT.jar
```

Para saber em que porta está o Postgres do simulador: `docker compose port postgres 5432`.

**Os tópicos Kafka não existem.**

```bash
docker compose up kafka-init
docker exec industry-kafka kafka-topics --bootstrap-server localhost:29092 --list
```

**Endpoints com `{id}` devolvem HTTP 400.**
O projecto compila com `-parameters` (exigido pelo Spring Boot 3.2+). Recompile com
`mvn clean package`.
