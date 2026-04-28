**✅ Recomendação COMPLETA e REALISTA para o vosso grupo (2 pessoas – 11 dias úteis até 1 de Maio)**

A vossa stack está **excelente** para o projecto (Spring Boot + Kafka + Docker + Angular + PostgreSQL). É exactamente o que os professores esperam ver num projecto de Sistemas Distribuídos.

Como o projecto está a **0%** e o deadline é curto, vamos focar num **MVP robusto** que cumpre **todos os requisitos obrigatórios** da versão 2 do enunciado, sem feature creep.

### 1. Estratégia Geral (o que vos salva)

| Prioridade | Objectivo | Tempo estimado |
| --- | --- | --- |
| MVP obrigatório | 4 microserviços + Kafka + Angular dashboard | 7 dias |
| Polimento + testes | Logs, validações, formulários, Docker | 3 dias |
| Apresentação (júnior) | Slides + demo ao vivo | 1 dia |

**Regra de ouro:**

- Cada microserviço tem a **sua própria base de dados PostgreSQL** (não partilham DB – cumpre a regra “sem acesso directo ao DB de outro serviço”).
- Comunicação: **Kafka** (assíncrono) + **REST** (síncrono para gestão).
- Pipeline: usa `CompletableFuture.delayedExecutor` ou `Thread.sleep` controlado (nunca produção instantânea).

### 2. Arquitectura Recomendada (simples e limpa)

**Monorepo Maven multi-módulo** (recomendado para 2 pessoas):

```
industria-4.0-simulator/
├── pom.xml (parent)
├── discovery-service/     (Eureka – opcional mas ajuda)
├── api-gateway/           (Spring Cloud Gateway – opcional)
├── raw-material-service/
├── processing-service/
├── component-service/
├── assembly-service/
├── inventory-service/
├── market-service/
├── frontend-angular/
├── docker-compose.yml
└── README.md
```

**Serviços mínimos obrigatórios (façam exactamente estes 4):**

1. **raw-material-service** (Matéria-prima)
2. **processing-service** (Processamento)
3. **component-service** (Componentes)
4. **assembly-service** (Montagem + Inventário + Mercado)

(Se der tempo, separa inventory e market – mas os 4 acima já cumprem o mínimo de 3 + stock + pedidos)

### 3. Divisão de Trabalho (Gama × Teresa)

**Gama:**

- Configuração do monorepo Maven + Docker Compose + Kafka + PostgreSQL
- Implementação completa do **Kafka** (tópicos, produtores, consumidores, eventos exactos do enunciado)
- Pipeline com tempo real
- Validação de BOM + compatibilidade
- Backend dos 4 serviços (80% do código)
- docker-compose.yml completo

**Teresa:**

- Angular 17 frontend (dashboard + formulários de configuração de pipelines)
- Todos os endpoints REST de gestão/monitorização (com Postman collections)
- Testes manuais + logs claros

**Trabalho em conjunto (daily 30 min):**

- Revisão de código + merge
- Testes integrados (Kafka + pipeline)

### 4. Plano Dia-a-Dia (20 de Abril → 1 de Maio)

**Semana 1 (20–26 Abril) – MVP Backend**

- Dia 20–21: Monorepo + Docker Compose + Kafka + 4 DBs PostgreSQL
- Dia 22–23: raw-material-service + processing-service (pipelines + eventos)
- Dia 24–25: component-service + assembly-service + BOM validation
- Dia 26: Inventory + Market (pedidos + stock)

**Semana 2 (27 Abril – 1 Maio) – Frontend + Polimento**

- Dia 27–28: Angular 17 (formulários de configuração de pipelines + dashboard em tempo real)
- Dia 29: Testes completos + logs + validações
- Dia 30: Docker tudo a funcionar com 1 comando
- Dia 1 Maio: Slides + ensaio da apresentação (Teresa
- apresenta)

### 5. Decisões Técnicas Importantes (para não perder tempo)

- **Kafka**: Use `spring-kafka` (já vem no Spring Boot 3). Crie exactamente os 5 tópicos do enunciado.
- **Pipeline tempo**: Crie uma classe `PipelineStep` com `durationMs` e use `CompletableFuture.delayedExecutor`.
- **Base de dados**: Cada serviço tem o seu `application.yml` com datasource próprio.
- **Angular**: Use standalone components + signals (Angular 17). Faça um dashboard bonito com:
    - Configuração de pipelines (formulário JSON)
    - Botão “Iniciar Produção de Carro”
    - Monitorização em tempo real (via WebSocket ou polling simples)
- **Eventos Kafka**: Payload deve seguir exactamente o modelo do enunciado (incluindo novo campo `purpose` da v2).

### 6. Dicas para a Apresentação (Teresa)

O júri vai gostar muito se o Teresa conseguir explicar:

- Porque usaram Kafka (assíncrono) + REST (síncrono)
- Como a pipeline simula latência real
- Como validam BOM e compatibilidade
- Como os serviços são independentes (sem acoplamento directo)

Dá para o Teresa fazer uma demo ao vivo de:

1. Configurar pipeline no Angular
2. Gerar um pedido de carro
3. Ver os eventos no Kafka + stock a actualizar

Quer que eu vos entregue **já hoje** algum dos seguintes pacotes prontos?

1. **docker-compose.yml** completo (Kafka + 5 PostgreSQL + serviços)
2. **Estrutura do monorepo Maven** + pom parent
3. **Template de um serviço** (ex: raw-material-service) com Kafka + pipeline
4. **Modelo de classes Java** (Component, BOM, PipelineStep, eventos)
5. **Angular 17 dashboard** inicial pronto a ligar

Diz-me qual queres primeiro e eu gero já (código completo, pronto a copiar).

Vocês conseguem entregar um projecto **muito bom** até 1 de Maio se seguirem este plano.

Bora? Qual o primeiro ficheiro que queres que eu prepare agora?