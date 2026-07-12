import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  RawMaterial,
  Component,
  BOM,
  MarketOrder,
  InventoryItem,
  AssembledProduct,
  ProductionRequest,
  ServiceKey,
  PipelineStep,
  WorkerPoolStatus,
  WorkerActivity,
  ExtractionConfig,
  CompatibleMaterial,
  ProductionRule,
  CustomerSimulatorConfig,
} from '../models';

/**
 * ApiService
 * Handles all HTTP communication with backend microservices
 * Base URLs are configured for each service
 */
@Injectable({
  providedIn: 'root',
})
export class ApiService {
  // Service base URLs (assume standard local ports as per docker-compose)
  private readonly BASE_URLS = {
    RAW_MATERIAL: 'http://localhost:8081/api/raw-materials',
    PROCESSING: 'http://localhost:8082/api/processing',
    COMPONENT: 'http://localhost:8083/api/components',
    ASSEMBLY: 'http://localhost:8084/api'
  };

  private rawMaterialUrl = this.BASE_URLS.RAW_MATERIAL;
  private processingUrl = this.BASE_URLS.PROCESSING;
  private componentUrl = this.BASE_URLS.COMPONENT;
  private assemblyUrl = this.BASE_URLS.ASSEMBLY;

  // Observable for loading state
  private loading$ = new BehaviorSubject<boolean>(false);
  loading = this.loading$.asObservable();

  // Observable for error state
  private error$ = new BehaviorSubject<string | null>(null);
  error = this.error$.asObservable();

  constructor(private http: HttpClient) {}

  // ============================================================================
  // RAW MATERIAL SERVICE
  // ============================================================================

  getRawMaterials(): Observable<RawMaterial[]> {
    return this.http
      .get<RawMaterial[]>(this.rawMaterialUrl)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  getRawMaterialByBatch(batchId: string): Observable<RawMaterial> {
    return this.http
      .get<RawMaterial>(`${this.rawMaterialUrl}/batch/${batchId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  createRawMaterial(material: RawMaterial): Observable<RawMaterial> {
    this.setLoading(true);
    return this.http
      .post<RawMaterial>(this.rawMaterialUrl, material)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  // ============================================================================
  // PROCESSING SERVICE
  // ============================================================================

  getProcessingBatches(): Observable<any[]> {
    return this.http
      .get<any[]>(this.processingUrl)
      .pipe(catchError((error) => this.handleError(error)));
  }

  startProcessing(batchId: string): Observable<any> {
    this.setLoading(true);
    return this.http
      .post(`${this.processingUrl}/start`, { batchId })
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  getProcessingStatus(batchId: string): Observable<any> {
    return this.http
      .get(`${this.processingUrl}/status/${batchId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getPipeline(): Observable<any[]> {
    return this.http
      .get<any[]>(`${this.processingUrl}/pipeline`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  savePipeline(steps: any[]): Observable<any[]> {
    this.setLoading(true);
    return this.http
      .post<any[]>(`${this.processingUrl}/pipeline`, steps)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  // ============================================================================
  // PORTAL DE CONFIGURAÇÕES (genérico, por microserviço)
  // ============================================================================

  /** Raiz REST de cada microserviço (todos expõem /pipeline e /workers). */
  private serviceRoot(service: ServiceKey): string {
    switch (service) {
      case 'raw-material':
        return this.rawMaterialUrl;
      case 'processing':
        return this.processingUrl;
      case 'component':
        return this.componentUrl;
      case 'assembly':
        return `${this.assemblyUrl}/assembly`;
    }
  }

  /** Etapas da pipeline (durationMs) de um microserviço. */
  getPipelineFor(service: ServiceKey): Observable<PipelineStep[]> {
    return this.http
      .get<PipelineStep[]>(`${this.serviceRoot(service)}/pipeline`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  savePipelineFor(service: ServiceKey, steps: PipelineStep[]): Observable<PipelineStep[]> {
    this.setLoading(true);
    return this.http
      .post<PipelineStep[]>(`${this.serviceRoot(service)}/pipeline`, steps)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  /** Número de Workers (Threads) activos e tamanho da fila. */
  getWorkers(service: ServiceKey): Observable<WorkerPoolStatus> {
    return this.http
      .get<WorkerPoolStatus>(`${this.serviceRoot(service)}/workers`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  /** Etapa que cada Worker está a executar neste instante. */
  getWorkerActivity(service: ServiceKey): Observable<WorkerActivity[]> {
    return this.http
      .get<WorkerActivity[]>(`${this.serviceRoot(service)}/workers/activity`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  setWorkers(service: ServiceKey, workerCount: number): Observable<WorkerPoolStatus> {
    this.setLoading(true);
    return this.http
      .put<WorkerPoolStatus>(`${this.serviceRoot(service)}/workers`, { workerCount })
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  // --- Camada 1: configuração de extracção (mapeamento genérico) ---

  getExtractionConfigs(): Observable<ExtractionConfig[]> {
    return this.http
      .get<ExtractionConfig[]>(`${this.rawMaterialUrl}/extraction-config`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  createExtractionConfig(config: ExtractionConfig): Observable<ExtractionConfig> {
    this.setLoading(true);
    return this.http
      .post<ExtractionConfig>(`${this.rawMaterialUrl}/extraction-config`, config)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  updateExtractionConfig(id: number, config: ExtractionConfig): Observable<ExtractionConfig> {
    this.setLoading(true);
    return this.http
      .put<ExtractionConfig>(`${this.rawMaterialUrl}/extraction-config/${id}`, config)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  deleteExtractionConfig(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.rawMaterialUrl}/extraction-config/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  // --- Regras de produção / BOM (Secção 6.3) ---

  /** Camadas com regras de produção configuráveis. */
  private ruleRoot(service: ServiceKey): string {
    switch (service) {
      case 'processing':
        return `${this.processingUrl}/production-rules`;
      case 'component':
        return `${this.componentUrl}/production-rules`;
      default:
        return `${this.assemblyUrl}/assembly/production-rules`;
    }
  }

  getProductionRules(service: ServiceKey): Observable<ProductionRule[]> {
    return this.http
      .get<ProductionRule[]>(this.ruleRoot(service))
      .pipe(catchError((error) => this.handleError(error)));
  }

  createProductionRule(service: ServiceKey, rule: ProductionRule): Observable<ProductionRule> {
    this.setLoading(true);
    return this.http
      .post<ProductionRule>(this.ruleRoot(service), rule)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  deleteProductionRule(service: ServiceKey, id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.ruleRoot(service)}/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  // --- Camada 6: simulação de clientes fictícios ---

  getCustomerSimulator(): Observable<CustomerSimulatorConfig> {
    return this.http
      .get<CustomerSimulatorConfig>(`${this.assemblyUrl}/market/customers`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateCustomerSimulator(config: CustomerSimulatorConfig): Observable<CustomerSimulatorConfig> {
    this.setLoading(true);
    return this.http
      .put<CustomerSimulatorConfig>(`${this.assemblyUrl}/market/customers`, config)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  /** Produtos que a fábrica sabe montar (vêm das regras da Camada 4). */
  getProductCatalog(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.assemblyUrl}/market/customers/catalog`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  // --- Camada 3: regras de compatibilidade BOM ---

  getCompatibleMaterials(): Observable<CompatibleMaterial[]> {
    return this.http
      .get<CompatibleMaterial[]>(`${this.componentUrl}/bom/compatible-materials`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  addCompatibleMaterial(material: CompatibleMaterial): Observable<CompatibleMaterial> {
    this.setLoading(true);
    return this.http
      .post<CompatibleMaterial>(`${this.componentUrl}/bom/compatible-materials`, material)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  deleteCompatibleMaterial(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.componentUrl}/bom/compatible-materials/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  // ============================================================================
  // COMPONENT SERVICE
  // ============================================================================

  getComponents(): Observable<Component[]> {
    return this.http
      .get<Component[]>(this.componentUrl)
      .pipe(catchError((error) => this.handleError(error)));
  }

  validateBOM(bomId: number, componentIds: number[]): Observable<any> {
    this.setLoading(true);
    return this.http
      .post(`${this.componentUrl}/validate-bom`, { bomId, componentIds })
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  getBOMs(): Observable<BOM[]> {
    return this.http
      .get<BOM[]>(`${this.componentUrl}/bom`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getBOM(bomId: number): Observable<BOM> {
    return this.http
      .get<BOM>(`${this.componentUrl}/bom/${bomId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  // ============================================================================
  // ASSEMBLY SERVICE
  // ============================================================================

  createMarketOrder(order: ProductionRequest = {
    productType: '',
    quantity: 1,
    bomVersion: 'v1.0.0',
    customerName: '',
    requiredDeliveryDate: new Date(),
    priority: 1,
  }): Observable<MarketOrder> {
    this.setLoading(true);
    return this.http
      .post<MarketOrder>(`${this.assemblyUrl}/market-orders`, order)
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  getMarketOrders(): Observable<MarketOrder[]> {
    return this.http
      .get<MarketOrder[]>(`${this.assemblyUrl}/market-orders`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getMarketOrderById(orderId: string): Observable<MarketOrder> {
    return this.http
      .get<MarketOrder>(`${this.assemblyUrl}/market-orders/${orderId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getInventory(): Observable<InventoryItem[]> {
    return this.http
      .get<InventoryItem[]>(`${this.assemblyUrl}/inventory`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getInventoryByProductType(productType: string): Observable<InventoryItem[]> {
    return this.http
      .get<InventoryItem[]>(`${this.assemblyUrl}/inventory/${productType}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  startAssembly(orderId: string): Observable<AssembledProduct> {
    this.setLoading(true);
    return this.http
      .post<AssembledProduct>(`${this.assemblyUrl}/assembly/start`, { orderId })
      .pipe(
        tap(() => this.setLoading(false)),
        catchError((error) => this.handleError(error))
      );
  }

  // ============================================================================
  // HEALTH CHECKS
  // ============================================================================

  healthCheck(service: string): Observable<any> {
    const url = `${this.getServiceUrl(service)}/health`;
    return this.http
      .get(url)
      .pipe(catchError((error) => this.handleError(error)));
  }

  private getServiceUrl(service: string): string {
    switch (service) {
      case 'raw-material':
        return this.rawMaterialUrl;
      case 'processing':
        return this.processingUrl;
      case 'component':
        return this.componentUrl;
      case 'assembly':
        return this.assemblyUrl;
      default:
        return '';
    }
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private setLoading(loading: boolean): void {
    this.loading$.next(loading);
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Ocorreu um erro';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erro: ${error.error.message}`;
    } else {
      // Extract detailed message from backend response if available
      const backendMessage = error.error?.message || error.message;
      errorMessage = `Erro ${error.status}: ${backendMessage}`;
    }
    this.error$.next(errorMessage);
    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  clearError(): void {
    this.error$.next(null);
  }
}
