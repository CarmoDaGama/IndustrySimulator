// Models and Interfaces for Industry Simulator

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

export interface InventoryItem {
  id: number;
  productType: string;
  componentType: string;
  batchId: string;
  quantity: number;
  unit: string;
  warehouseLocation: string;
  status: string;
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

export interface KafkaEvent {
  eventId: string;
  eventType: string;
  timestamp: Date;
  batchId: string;
  status: string;
  details: any;
  purpose: string;
}

export interface ProductionRequest {
  productType: string;
  quantity: number;
  bomVersion: string;
  customerName: string;
  requiredDeliveryDate: Date;
  priority: number;
}
