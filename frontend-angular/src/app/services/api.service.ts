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
  // Service base URLs
  private rawMaterialUrl = 'http://localhost:8081/api/raw-materials';
  private processingUrl = 'http://localhost:8082/api/processing';
  private componentUrl = 'http://localhost:8083/api/components';
  private assemblyUrl = 'http://localhost:8084/api/assembly';

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

  createMarketOrder(order: ProductionRequest): Observable<MarketOrder> {
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
      errorMessage = `Código do erro: ${error.status}\nMensagem: ${error.message}`;
    }
    this.error$.next(errorMessage);
    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  clearError(): void {
    this.error$.next(null);
  }
}
