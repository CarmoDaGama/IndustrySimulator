import { Component, OnInit, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { EventService } from '../../services/event.service';
import { WebSocketService, OrderStatusEvent } from '../../services/websocket.service';
import { ProductionRequest } from '../../models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * OrderFormComponent
 * Allows customers to create market orders for car production
 * Includes product type, quantity, and delivery date configuration
 * Also provides real-time order status updates via WebSocket
 */
@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="order-form-container">
      <div class="form-header">
        <h2><i class="material-icons">assignment</i> Novo pedido de produção</h2>
        <p class="description">Crie um novo pedido de mercado para a produção do veículo</p>
      </div>

      <div class="form-content">
        <form (ngSubmit)="submitOrder()">
          <!-- Customer Information -->
          <div class="form-section">
            <h3>Informações do cliente</h3>

            <div class="form-group">
              <label for="customerName">Nome do cliente *</label>
              <input
                id="customerName"
                type="text"
                [(ngModel)]="orderData.customerName"
                name="customerName"
                placeholder="Informe o nome do cliente"
                required
                class="input-field"
              />
            </div>
          </div>

          <!-- Product Configuration -->
          <div class="form-section">
            <h3>Configuração do produto</h3>

            <div class="form-group">
              <label for="productType">Tipo de produto *</label>
              <select
                id="productType"
                [(ngModel)]="orderData.productType"
                name="productType"
                required
                class="input-field"
              >
                <option value="">Selecione um tipo de produto</option>
                <option value="SEDAN">Sedã</option>
                <option value="SUV">SUV</option>
                <option value="TRUCK">Caminhão</option>
                <option value="SPORTS">Carro esportivo</option>
              </select>
            </div>

            <div class="form-group">
              <label for="quantity">Quantidade *</label>
              <input
                id="quantity"
                type="number"
                [(ngModel)]="orderData.quantity"
                name="quantity"
                min="1"
                max="100"
                placeholder="1"
                required
                class="input-field"
              />
            </div>

            <div class="form-group">
              <label for="bomVersion">Versão da BOM *</label>
              <input
                id="bomVersion"
                type="text"
                [(ngModel)]="orderData.bomVersion"
                name="bomVersion"
                placeholder="Ex.: v1.2.3"
                required
                class="input-field"
              />
            </div>
          </div>

          <!-- Delivery Configuration -->
          <div class="form-section">
            <h3>Configuração de entrega</h3>

            <div class="form-group">
              <label for="deliveryDate">Data de entrega necessária *</label>
              <input
                id="deliveryDate"
                type="date"
                [(ngModel)]="deliveryDateStr"
                name="deliveryDate"
                required
                class="input-field"
              />
            </div>

            <div class="form-group">
              <label for="priority">Nível de prioridade</label>
              <select
                id="priority"
                [(ngModel)]="orderData.priority"
                name="priority"
                class="input-field"
              >
                <option value="1">Baixa</option>
                <option value="2">Média</option>
                <option value="3">Alta</option>
                <option value="4">Urgente</option>
              </select>
            </div>
          </div>

          <!-- Order Summary -->
          <div class="order-summary">
            <h3>Resumo do pedido</h3>
            <div class="summary-grid">
              <div class="summary-item">
                <span class="label">Cliente:</span>
                <span class="value">{{ orderData.customerName || 'N/D' }}</span>
              </div>
              <div class="summary-item">
                <span class="label">Produto:</span>
                <span class="value">{{ translateProductType(orderData.productType) || 'N/D' }}</span>
              </div>
              <div class="summary-item">
                <span class="label">Quantidade:</span>
                <span class="value">{{ orderData.quantity || '0' }}</span>
              </div>
              <div class="summary-item">
                <span class="label">Prioridade:</span>
                <span class="value priority" [class]="getPriorityClass(orderData.priority)">
                  {{ getPriorityLabel(orderData.priority) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="isLoading()">
              <i class="material-icons">{{ isLoading() ? 'hourglass_top' : 'check_circle' }}</i>
              {{ isLoading() ? 'Processando...' : 'Criar pedido' }}
            </button>
            <button type="reset" class="btn btn-secondary" (click)="resetForm()">
              <i class="material-icons">refresh</i>
              Limpar
            </button>
          </div>

          <!-- Messages -->
          <div *ngIf="successMessage()" class="message success">
            {{ successMessage() }}
          </div>

          <div *ngIf="errorMessage()" class="message error">
            {{ errorMessage() }}
          </div>

          <!-- Order Status Live Updates -->
          <div *ngIf="createdOrderId()" class="status-section">
            <h3><i class="material-icons">info</i> Status em tempo real do pedido</h3>
            
            <div class="status-header">
              <div class="status-info">
                <span class="label">ID do Pedido:</span>
                <span class="value">{{ createdOrderId() }}</span>
              </div>
              <div class="status-badge" [class]="'status-' + orderStatus().toLowerCase()">
                {{ orderStatus() || 'PENDING' }}
              </div>
              <div class="connection-indicator" [class.connected]="wsConnected()">
                <i class="material-icons">{{ wsConnected() ? 'cloud_done' : 'cloud_off' }}</i>
                {{ wsConnected() ? 'Conectado' : 'Desconectado' }}
              </div>
            </div>

            <div *ngIf="orderStatusUpdates().length > 0" class="status-updates">
              <h4>Histórico de atualizações:</h4>
              <div class="updates-list">
                <div *ngFor="let update of orderStatusUpdates()" class="update-item" [class]="'status-' + update.status.toLowerCase()">
                  <div class="update-status">{{ update.status }}</div>
                  <div class="update-message">{{ update.message }}</div>
                  <div class="update-time">{{ update.updatedAt | date:'short' }}</div>
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .order-form-container {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        overflow: hidden;
      }

      .form-header {
        background: var(--primary-dark);
        color: white;
        padding: 1.5rem;
        border-bottom: 4px solid var(--primary-dark);
      }

      .form-header h2 {
        margin: 0 0 0.5rem 0;
        font-size: 1.5rem;
      }

      .description {
        margin: 0;
        opacity: 0.9;
        font-size: 0.9rem;
      }

      .form-content {
        padding: 2rem;
      }

      form {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .form-section {
        border-bottom: 1px solid #bfdbfe;
        padding-bottom: 1.5rem;
      }

      .form-section:last-of-type {
        border-bottom: none;
      }

      .form-section h3 {
        margin: 0 0 1rem 0;
        color: #1e3a8a;
        font-size: 1.1rem;
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      .form-group:last-child {
        margin-bottom: 0;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 600;
        color: #475569;
        font-size: 0.95rem;
      }

      .input-field {
        width: 100%;
        padding: 0.75rem;
        border: 1px solid #bfdbfe;
        border-radius: 4px;
        font-size: 1rem;
        box-sizing: border-box;
        transition: border-color 0.3s;
      }

      .input-field:focus {
        outline: none;
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
      }

      .order-summary {
        background: #eff6ff;
        padding: 1.5rem;
        border-radius: 6px;
        border-left: 4px solid var(--primary-color);
      }

      .order-summary h3 {
        margin: 0 0 1rem 0;
        color: #333;
      }

      .summary-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
      }

      .summary-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        background: white;
        border-radius: 4px;
      }

      .summary-item .label {
        font-weight: 600;
        color: #475569;
      }

      .summary-item .value {
        color: #1e3a8a;
      }

      .priority {
        padding: 0.25rem 0.75rem;
        border-radius: 20px;
        font-size: 0.85rem;
        font-weight: 600;
      }

      .priority.low {
        background: #dbeafe;
        color: #1d4ed8;
      }

      .priority.medium {
        background: #eff6ff;
        color: #2563eb;
      }

      .priority.high {
        background: #bfdbfe;
        color: #1e40af;
      }

      .priority.urgent {
        background: #dbeafe;
        color: #1e3a8a;
      }

      .form-actions {
        display: flex;
        gap: 1rem;
        margin-top: 1.5rem;
      }

      .btn {
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 4px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.3s;
        font-weight: 600;
      }

      .btn-primary {
        background: var(--primary-color);
        color: white;
        flex: 1;
      }

      .btn-primary:hover:not(:disabled) {
        background: var(--primary-dark);
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
      }

      .btn-primary:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .btn-secondary {
        background: var(--primary-dark);
        color: white;
      }

      .btn-secondary:hover {
        background: var(--primary-color);
      }

      .message {
        padding: 1rem;
        border-radius: 4px;
        margin-top: 1rem;
      }

      .message.success {
        background: #dbeafe;
        color: #1d4ed8;
        border-left: 4px solid var(--primary-color);
      }

      .message.error {
        background: #eff6ff;
        color: #1e40af;
        border-left: 4px solid var(--primary-dark);
      }

      .status-section {
        background: #f0f9ff;
        border: 2px solid var(--primary-color);
        border-radius: 6px;
        padding: 1.5rem;
        margin-top: 1.5rem;
      }

      .status-section h3 {
        margin: 0 0 1rem 0;
        color: var(--primary-dark);
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .status-section .material-icons {
        font-size: 1.3rem;
      }

      .status-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: white;
        border-radius: 4px;
        margin-bottom: 1rem;
        flex-wrap: wrap;
        gap: 1rem;
      }

      .status-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }

      .status-info .label {
        font-size: 0.85rem;
        color: #64748b;
        font-weight: 600;
      }

      .status-info .value {
        font-family: monospace;
        font-size: 0.95rem;
        color: #1e293b;
        font-weight: 600;
      }

      .status-badge {
        padding: 0.5rem 1rem;
        border-radius: 20px;
        font-weight: 700;
        font-size: 0.9rem;
        text-transform: uppercase;
      }

      .status-pending {
        background: #fef08a;
        color: #78350f;
      }

      .status-processing {
        background: #bfdbfe;
        color: #1e3a8a;
      }

      .status-assembled {
        background: #bbf7d0;
        color: #065f46;
      }

      .status-shipped {
        background: #c7d2fe;
        color: #3730a3;
      }

      .status-completed {
        background: #86efac;
        color: #15803d;
      }

      .status-failed {
        background: #fca5a5;
        color: #7f1d1d;
      }

      .connection-indicator {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        background: #f1f5f9;
        font-size: 0.9rem;
        color: #64748b;
      }

      .connection-indicator.connected {
        background: #dcfce7;
        color: #166534;
      }

      .connection-indicator .material-icons {
        font-size: 1.1rem;
      }

      .status-updates {
        background: white;
        border-radius: 4px;
        padding: 1rem;
      }

      .status-updates h4 {
        margin: 0 0 1rem 0;
        font-size: 0.95rem;
        color: #475569;
      }

      .updates-list {
        display: flex;
        flex-direction: column-reverse;
        gap: 0.75rem;
      }

      .update-item {
        padding: 0.75rem;
        border-left: 4px solid #cbd5e1;
        border-radius: 2px;
        background: #f8fafc;
      }

      .update-item.status-pending {
        border-left-color: #fbbf24;
      }

      .update-item.status-processing {
        border-left-color: #60a5fa;
      }

      .update-item.status-assembled {
        border-left-color: #34d399;
      }

      .update-item.status-shipped {
        border-left-color: #818cf8;
      }

      .update-item.status-completed {
        border-left-color: #4ade80;
      }

      .update-item.status-failed {
        border-left-color: #f87171;
      }

      .update-status {
        font-weight: 700;
        font-size: 0.9rem;
        margin-bottom: 0.25rem;
      }

      .update-message {
        font-size: 0.9rem;
        color: #475569;
        margin-bottom: 0.25rem;
      }

      .update-time {
        font-size: 0.8rem;
        color: #94a3b8;
      }

      @media (max-width: 768px) {
        .form-content {
          padding: 1rem;
        }

        .summary-grid {
          grid-template-columns: 1fr;
        }

        .form-actions {
          flex-direction: column;
        }

        .btn {
          width: 100%;
        }
      }
    `,
  ],
})
export class OrderFormComponent implements OnInit, OnDestroy {
  orderData: ProductionRequest = {
    productType: '',
    quantity: 1,
    bomVersion: '',
    customerName: '',
    requiredDeliveryDate: new Date(),
    priority: 1,
  };

  deliveryDateStr = '';
  isLoading = signal(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');
  createdOrderId = signal<string>('');
  orderStatus = signal<string>('');
  orderStatusUpdates = signal<OrderStatusEvent[]>([]);
  wsConnected = signal(false);

  private destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private eventService: EventService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.setDefaultDeliveryDate();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private connectWebSocket(): void {
    this.webSocketService.connect();
    
    this.webSocketService.isConnected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(connected => {
        this.wsConnected.set(connected);
      });

    this.webSocketService.orderStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(statusEvent => {
        // If this is our created order, track the update
        if (this.createdOrderId() && statusEvent.orderId === this.createdOrderId()) {
          this.orderStatus.set(statusEvent.status);
          const updates = [statusEvent, ...this.orderStatusUpdates()];
          this.orderStatusUpdates.set(updates.slice(0, 10)); // Keep last 10 updates
        }
      });
  }

  private setDefaultDeliveryDate(): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 7);
    this.deliveryDateStr = tomorrow.toISOString().split('T')[0];
  }

  submitOrder(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading.set(true);
    this.orderData.requiredDeliveryDate = new Date(this.deliveryDateStr);

    this.apiService.createMarketOrder(this.orderData).subscribe({
      next: (response) => {
        this.createdOrderId.set(response.orderId);
        this.orderStatus.set('PENDING');
        this.orderStatusUpdates.set([]);
        
        // Subscribe to this specific order's updates
        this.webSocketService.subscribeToOrder(response.orderId);
        
        this.successMessage.set(`Pedido criado com sucesso! ID: ${response.orderId}`);
        this.resetForm();
        this.isLoading.set(false);
        setTimeout(() => this.successMessage.set(''), 5000);
      },
      error: (error) => {
        this.errorMessage.set(`Erro ao criar o pedido: ${error.message}`);
        this.isLoading.set(false);
        setTimeout(() => this.errorMessage.set(''), 5000);
      },
    });
  }

  private validateForm(): boolean {
    if (!this.orderData.customerName.trim()) {
      this.errorMessage.set('O nome do cliente é obrigatório');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return false;
    }

    if (!this.orderData.productType) {
      this.errorMessage.set('Selecione um tipo de produto');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return false;
    }

    if (this.orderData.quantity < 1) {
      this.errorMessage.set('A quantidade deve ser de pelo menos 1');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return false;
    }

    if (!this.orderData.bomVersion.trim()) {
      this.errorMessage.set('A versão da BOM é obrigatória');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return false;
    }

    return true;
  }

  resetForm(): void {
    this.orderData = {
      productType: '',
      quantity: 1,
      bomVersion: '',
      customerName: '',
      requiredDeliveryDate: new Date(),
      priority: 1,
    };
    this.setDefaultDeliveryDate();
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  getPriorityLabel(priority: number): string {
    const labels: { [key: number]: string } = {
      1: 'Baixa',
      2: 'Média',
      3: 'Alta',
      4: 'Urgente',
    };
    return labels[priority] || 'N/D';
  }

  getPriorityClass(priority: number): string {
    const classes: { [key: number]: string } = {
      1: 'low',
      2: 'medium',
      3: 'high',
      4: 'urgent',
    };
    return classes[priority] || '';
  }

  translateProductType(productType: string): string {
    const labels: { [key: string]: string } = {
      SEDAN: 'Sedã',
      SUV: 'SUV',
      TRUCK: 'Caminhão',
      SPORTS: 'Carro esportivo',
    };

    return labels[productType] || productType;
  }
}
