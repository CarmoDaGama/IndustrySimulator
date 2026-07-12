// Models and Interfaces for Industry Simulator

/** Microserviços (camadas) configuráveis no portal. */
export type ServiceKey = 'raw-material' | 'processing' | 'component' | 'assembly';

export interface ServiceInfo {
  key: ServiceKey;
  label: string;
  layer: string;
  hasPipeline: boolean;
}

export const SERVICES: ServiceInfo[] = [
  { key: 'raw-material', label: 'Extracção', layer: 'Camada 1', hasPipeline: true },
  { key: 'processing', label: 'Processamento', layer: 'Camada 2', hasPipeline: true },
  { key: 'component', label: 'Componentes', layer: 'Camada 3', hasPipeline: true },
  { key: 'assembly', label: 'Montagem', layer: 'Camada 4', hasPipeline: true },
];

/** Estado da pool de Workers (Threads) de um microserviço. */
export interface WorkerPoolStatus {
  workerCount: number;
  queueSize?: number;
}

/** O que um Worker está a fazer neste instante (etapa em execução). */
export interface WorkerActivity {
  workerName: string;
  /** BLOCKED = à espera de insumos da camada anterior (bloqueio por escassez). */
  state: 'RUNNING' | 'BLOCKED' | 'IDLE';
  currentStep: string | null;
  stepIndex: number;
  totalSteps: number;
  stepDurationMs: number;
  stepElapsedMs: number;
  batchIds: string | null;
}

/** Config genérica do recurso extraído autonomamente pela Camada 1. */
export interface ExtractionConfig {
  id?: number;
  materialName: string;
  materialType: string;
  quantityPerCycle: number;
  unit: string;
  factory: string;
  targetProduct: string;
  targetComponent: string;
  description: string;
  active: boolean;
}

/** Um insumo exigido por uma regra de produção. */
export interface ProductionRuleInput {
  id?: number;
  inputMaterial: string;
  inputType?: string;
  inputQuantity: number;
}

/**
 * Regra de produção (Secção 6.3): mapeamento genérico e árvore BOM.
 * Com vários inputs, cobre tanto "Ferro ×2 → Aço ×1" como
 * "Motor ×1 + Pneus ×4 → Carro ×1".
 */
export interface ProductionRule {
  id?: number;
  outputMaterial: string;
  outputType?: string;
  outputQuantity: number;
  factory?: string;
  targetProduct?: string;
  targetComponent?: string;
  description?: string;
  active: boolean;
  inputs: ProductionRuleInput[];
}

/** Simulação de clientes fictícios (Camada 6). */
export interface CustomerSimulatorConfig {
  customerCount: number;
  thinkTimeMs: number;
  minQuantity: number;
  maxQuantity: number;
}

/** Regra de compatibilidade BOM (mapeamento genérico, vinda da BD). */
export interface CompatibleMaterial {
  id?: number;
  materialType: string;
}

export interface PipelineStep {
  id: number;
  stepName: string;
  stepOrder: number;
  durationMs: number;
  description: string;
  isActive: boolean;
}

export interface PipelineConfig {
  name: string;
  description: string;
  steps: PipelineStep[];
  estimatedDuration: number;
}

export interface RawMaterial {
  id: number;
  materialName: string;
  materialType: string;
  batchId: string;
  quantity: number;
  unit: string;
  supplier: string;
  qualityScore: number;
  status: string;
}

export interface Component {
  id: number;
  componentName: string;
  componentType: string;
  version: string;
  quantity: number;
  unit: string;
  compatibilityStatus: string;
  isCompatible: boolean;
  qualityScore: number;
  batchId: string;
  status: string;
}

export interface BOM {
  id: number;
  bomName: string;
  bomVersion: string;
  productType: string;
  description: string;
  isActive: boolean;
  requirements: BOMRequirement[];
}

export interface BOMRequirement {
  id: number;
  componentType: string;
  requiredQuantity: number;
  compatibilityRule: string;
  isOptional: boolean;
}

export interface MarketOrder {
  id: number;
  orderId: string;
  productType: string;
  bomVersion: string;
  requestedQuantity: number;
  status: string;
  priority: number;
  customerName: string;
  requiredDeliveryDate: Date;
}

/** Stock global (Camada 5). Espelha o que o assembly-service devolve. */
export interface InventoryItem {
  id: number;
  productId: string;
  productName: string;
  /** Total produzido e armazenado. */
  quantity: number;
  /** Reservado por encomendas já alocadas. */
  reservedQuantity: number;
  /** Livre para novas encomendas (quantity - reserved). */
  availableQuantity: number;
  location: string;
  lastUpdated: string;
}

export interface AssembledProduct {
  id: number;
  productId: string;
  orderId: string;
  bomId: string;
  status: string;
  assemblyStartTime: Date;
  assemblyEndTime: Date;
  qualityCheckPassed: boolean;
  notes: string;
}

/** Envelope padrão do enunciado: {eventId, eventType, timestamp, payload}. */
export interface KafkaEvent {
  eventId: string;
  eventType: string;
  /** epoch (segundos). */
  timestamp: number;
  /** Nó da árvore (id, name, purpose, producer, components) ou payload da notificação. */
  payload: any;
}

export interface ProductionRequest {
  productType: string;
  quantity: number;
  bomVersion: string;
  customerName: string;
  requiredDeliveryDate: Date;
  priority: number;
}
