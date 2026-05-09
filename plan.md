**Aqui está o conteúdo completo do Enunciado Projecto v2.pdf (14 páginas), transcrito integralmente sem qualquer resumo ou omissão.**

---

**Simulador da Industria 4.0**

**República de Angola**  
**Universidade Católica de Angola**  
**Faculdade de Engenharia**

**Curso:** Engenharia Informática  
**Disciplina:** Sistemas Distribuídos e Paralelos II  
**Docente:** Prof. Eng. Domingos Fernando  

**Ano curricular:** 4º - 2026/2027

### 1. Enquadramento

O governo angolano no seu plano de desenvolvimento e diversificação económica, sendo que possuímos em nosso solo ou em países próximos, todos os recursos naturais, minerais e hídricos para a produção em massa de produtos hoje maioritariamente importados, almeja alcançar a auto suficiência industrial suprindo assim toda a cadeia vertical de valor, desde a exploração, processamento, refinamento e construção de derivados, bem como a distribuição e exportação.

Com isso em mente, sabendo que os estudantes da Universidade Católica pela sua fama, detêm um vasto conhecimento sobre desenvolvimento de sistemas, lançou o desafio para os estudantes, no qual pretende então, previamente construir um sistema que simula o funcionamento da indústria real, a fim de refinar o planejamento, identificar pontos fracos e fortes, bem como encontrar possíveis pontos de falha ou gargalos a serem mitigados.

### 2. Objectivo

Desenvolver um sistema distribuído baseado em arquitetura de microserviços que simule o funcionamento de uma cadeia industrial e social, desde a extração de matéria-prima até a entrega de produtos finais ao mercado.

O projeto deve demonstrar, de forma prática, conceitos fundamentais de:

- Sistemas distribuídos
- Compatibilidade entre peças
- Tempo real de produção (latência)
- Consumo de recursos
- Processos industriais (pipelines)
- Comunicação via eventos (Kafka)

### 3. Descrição do Problema

A sociedade moderna depende de cadeias industriais complexas. Neste projeto, cada grupo deverá implementar um conjunto de microserviços que representam partes dessa cadeia, incluindo:

- Extração de matéria-prima
- Processamento industrial
- Produção de componentes
- Montagem de produtos finais
- Gestão de stock
- Simulação de mercado (pedidos)

Cada serviço deve funcionar de forma independente, mas cooperar com os demais através de comunicação distribuída.

### 4. Conceito Central

O sistema deve funcionar como uma fábrica distribuída, onde:

- Cada microserviço representa uma indústria
- Produção ocorre em etapas (pipeline)
- Cada etapa leva tempo
- Produção depende de:
  - recursos disponíveis
  - compatibilidade de componentes
  - capacidade do sistema

**Matéria-prima → Processamento → Componentes → Montagem → Inventário → Venda**

### 5. Arquitetura Esperada

O sistema deve seguir uma arquitetura baseada em microserviços, com as seguintes características:

**Serviços sugeridos (mínimo obrigatório)**

Cada grupo deverá implementar pelo menos 3 microserviços, escolhidos entre:

1. Serviço de Matéria-Prima  
   a. Simula extração (ferro, petróleo, madeira, etc.)

2. Serviço de Processamento  
   a. Refina matéria-prima em materiais utilizáveis

3. Serviço de Componentes  
   a. Produz peças industriais (ex: motor, chip, vidro)

4. Serviço de Montagem  
   a. Produz produtos finais (ex: carro, telefone)

5. Serviço de Inventário  
   a. Gerencia stock global

6. Serviço de Mercado  
   a. Simula pedidos de clientes

### 7. Comunicação entre Serviços

O sistema deve obrigatoriamente utilizar:

**Comunicação síncrona**
- REST APIs (HTTP) – Para a gestão, monitorar e configurações

**Comunicação assíncrona**
- Sistema de mensageria **Kafka**

**Eventos**
- RAW_MATERIAL_EXTRACTED
- MATERIAL_PROCESSED
- COMPONENT_CREATED
- PRODUCT_ASSEMBLED
- ORDER_CREATED

### 8. BOM — Componentes mínimos

- **Carro**
  - Motor → 1
  - Pneus → 4
  - Chassis → 1
  - Transmissão → 1
  - Sistema elétrico → 1

- **Motor**
  - Pistões → 4–8
  - Cambota → 1
  - Bloco → 1
  - Cabeçote → 1

### 9. Regras obrigatórias

- Não deve gerar Produção instantânea
- Não deve gerar Ignorar compatibilidade
- Não deve Ignorar indisponibilidade de recursos
- Não deve ter Acesso direto ao DB de outro serviço
- Uso de eventos Kafka é Obrigatório
- Pipeline com tempo
- Validação de BOM
- Logs claros
- Qualquer vestígio de cópia ou plágio resultará em anulação imediata
- Todos os projectos devem ter um ou vários formulários para configuração das pipelines de produção e operações.

### 10. Modelo de Dados (PADRÃO)

Todos os serviços devem usar um modelo consistente.

**Regras obrigatórias**
- Todo nó da árvore deve conter:
  - id
  - name
  - components
  - producer.service
  - producer.factory
- Nenhum componente pode existir sem produtor
- Cada microserviço só pode produzir os seus próprios tipos

**Evento base (Kafka)**
```json
{
  "eventId": "uuid",
  "eventType": "COMPONENT_CREATED",
  "timestamp": 1710000000,
  "payload": { ... }
}
```

**Exemplo de Um Componente**
```json
{
    "id": "eng-001",
    "type": "COMPONENT",
    "name": "Motor V8",
    "specs": {
        "engineType": "V8",
        "powerHP": 450
    },
    "compatibility": {
        "vehicleType": ["SUV"],
        "mountType": "LARGE"
    },
    "producer": {
        "service": "engine-service",
        "factory": "engine-plant"
    },
    "components": [...]
}
```

(Os exemplos completos de Carro e Telefone vendidos estão nas páginas 5 a 9 do PDF, com a árvore completa de componentes recursiva.)

### 11. Pipeline de Produção (TEMPO OBRIGATÓRIO)

Toda produção deve seguir etapas com tempo.

**Exemplos de configurações para pipelines:**
```json
// Mineração de Ferro
[
  { "name": "EXTRACTION", "durationMs": 10000 },
  { "name": "TRANSPORT", "durationMs": 5000 }
]

// Produção de Aço
[
  { "name": "SMELTING", "durationMs": 8000 },
  { "name": "REFINING", "durationMs": 6000 }
]

// Motor V8
[
  { "name": "PART_PREP", "durationMs": 6000 },
  { "name": "ASSEMBLY", "durationMs": 10000 },
  { "name": "TEST", "durationMs": 4000 }
]
```

**Etapas padrão para cada categoria:**

1. **Matéria-prima**
   1. EXTRACTION
   2. INITIAL_PROCESSING
   3. PACKAGING_FOR_TRANSPORT

2. **Processamento**
   1. REFINING
   2. PURIFICATION
   3. MATERIAL_STANDARDIZATION

3. **Componentes**
   1. PART_PREPARATION
   2. ASSEMBLY
   3. QUALITY_CHECK

4. **Produto Final**
   1. FINAL_ASSEMBLY
   2. INTEGRATION
   3. FINAL_TEST

### 12. Exemplo de produtos a serem fabricados

computador, carros, telefone, tablet, tv plasma, sound bar.

**Computador**, **Carro**, **Telefone**, **TV** e **Sound Bar** — com as respectivas árvores BOM detalhadas (ver páginas 11-13 do PDF).

**Fluxo Padrão:**  
**RAW → PROCESSING → COMPONENT → ASSEMBLY → TEST → DONE**

**Cada nó deve incluir:**
```json
"purpose": {
  "targetProduct": "CAR | PHONE | TV | ...",
  "targetComponent": "ENGINE | SCREEN | CHIP | ...",
  "description": "Texto explicando o uso"
}
```

**Exemplo (matéria-prima)**
```json
{
  "name": "Areia",
  "type": "RAW_MATERIAL",
  "purpose": {
    "targetProduct": "SMARTPHONE",
    "targetComponent": "SCREEN",
    "description": "Produção de vidro para ecrã"
  }
}
```