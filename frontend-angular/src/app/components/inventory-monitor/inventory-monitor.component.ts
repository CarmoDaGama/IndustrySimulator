import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { EventService } from '../../services/event.service';
import { InventoryItem } from '../../models';

/**
 * InventoryMonitorComponent
 * Real-time monitoring of inventory levels across all products and components
 * Displays stock status and tracks inventory updates via Kafka events
 */
@Component({
  selector: 'app-inventory-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="inventory-monitor-container">
      <div class="monitor-header">
        <h2><i class="material-icons">inventory_2</i> Monitor de estoque</h2>
        <p class="description">Monitoramento em tempo real dos níveis de estoque</p>
      </div>

      <div class="monitor-content">
        <!-- Filter Bar -->
        <div class="filter-bar">
          <div class="filter-group">
            <label>Última atualização:</label>
            <span class="last-update">{{ getLastUpdate() }}</span>
          </div>
          <button (click)="refreshInventory()" class="btn btn-refresh">
            <i class="material-icons">refresh</i>
            Atualizar
          </button>
        </div>

        <!-- Inventory Table -->
        <div class="inventory-table-wrapper">
          <div *ngIf="isLoading()" class="loading">
            <p>Carregando dados do estoque...</p>
          </div>

          <div *ngIf="!isLoading() && inventoryItems().length === 0" class="empty-state">
            <p>Nenhum item de estoque encontrado</p>
          </div>

          <table *ngIf="!isLoading() && inventoryItems().length > 0" class="inventory-table">
            <thead>
              <tr>
                <th>Tipo de produto</th>
                <th>Componente</th>
                <th>ID do lote</th>
                <th>Quantidade</th>
                <th>Unidade</th>
                <th>Local</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of inventoryItems()" [class.low-stock]="item.quantity < 10">
                <td><strong>{{ item.productType }}</strong></td>
                <td>{{ item.componentType || 'N/D' }}</td>
                <td class="batch-id">{{ item.batchId || 'N/D' }}</td>
                <td class="quantity" [class.critical]="item.quantity < 5">
                  <strong>{{ item.quantity.toFixed(2) }}</strong>
                </td>
                <td>{{ item.unit }}</td>
                <td>{{ item.warehouseLocation || 'Desconhecido' }}</td>
                <td>
                  <span class="status-badge" [class]="getStatusClass(item.status)">
                    {{ translateStatus(item.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Statistics -->
        <div class="inventory-stats">
          <div class="stat-card">
            <div class="stat-value">{{ inventoryItems().length }}</div>
            <div class="stat-label">Itens totais</div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ getTotalQuantity().toFixed(0) }}</div>
            <div class="stat-label">Estoque total</div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ getLowStockCount() }}</div>
            <div class="stat-label">Itens com estoque baixo</div>
          </div>

          <div class="stat-card">
            <div class="stat-value">{{ getCriticalStockCount() }}</div>
            <div class="stat-label">Itens críticos</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .inventory-monitor-container {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        overflow: hidden;
      }

      .monitor-header {
        background: var(--primary-dark);
        color: white;
        padding: 1.5rem;
        border-bottom: 4px solid var(--primary-dark);
      }

      .monitor-header h2 {
        margin: 0 0 0.5rem 0;
        font-size: 1.5rem;
      }

      .description {
        margin: 0;
        opacity: 0.9;
        font-size: 0.9rem;
      }

      .monitor-content {
        padding: 1.5rem;
      }

      .filter-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
        padding: 1rem;
        background: #eff6ff;
        border-radius: 6px;
      }

      .filter-group {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }

      .filter-group label {
        font-weight: 600;
        color: #555;
      }

      .last-update {
        color: var(--primary-color);
        font-weight: 600;
      }

      .btn-refresh {
        padding: 0.5rem 1rem;
        background: var(--primary-color);
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s;
      }

      .btn-refresh:hover {
        background: var(--primary-dark);
        transform: translateY(-2px);
      }

      .loading {
        text-align: center;
        padding: 2rem;
        color: #64748b;
      }

      .empty-state {
        text-align: center;
        padding: 2rem;
        color: #999;
      }

      .inventory-table-wrapper {
        overflow-x: auto;
        margin-bottom: 2rem;
      }

      .inventory-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.95rem;
      }

      .inventory-table thead {
        background: #dbeafe;
        border-bottom: 2px solid var(--primary-color);
      }

      .inventory-table th {
        padding: 1rem;
        text-align: left;
        font-weight: 600;
        color: #1e3a8a;
      }

      .inventory-table td {
        padding: 1rem;
        border-bottom: 1px solid #bfdbfe;
      }

      .inventory-table tbody tr:hover {
        background: #eff6ff;
      }

      .inventory-table tbody tr.low-stock {
        background: #eff6ff;
      }

      .batch-id {
        font-family: monospace;
        font-size: 0.9rem;
        color: #475569;
      }

      .quantity {
        font-size: 1.1rem;
        color: var(--primary-color);
      }

      .quantity.critical {
        color: var(--primary-dark);
        background: #dbeafe;
        padding: 0.25rem 0.5rem;
        border-radius: 3px;
      }

      .status-badge {
        padding: 0.25rem 0.75rem;
        border-radius: 20px;
        font-size: 0.85rem;
        font-weight: 600;
      }

      .status-badge.available {
        background: #dbeafe;
        color: #1d4ed8;
      }

      .status-badge.reserved {
        background: #eff6ff;
        color: #2563eb;
      }

      .status-badge.unavailable {
        background: #bfdbfe;
        color: #1e40af;
      }

      .inventory-stats {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
      }

      .stat-card {
        background: var(--primary-color);
        color: white;
        padding: 1.5rem;
        border-radius: 6px;
        text-align: center;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .stat-value {
        font-size: 2rem;
        font-weight: 700;
        margin-bottom: 0.5rem;
      }

      .stat-label {
        font-size: 0.9rem;
        opacity: 0.9;
      }

      @media (max-width: 768px) {
        .filter-bar {
          flex-direction: column;
          gap: 1rem;
          align-items: flex-start;
        }

        .inventory-table {
          font-size: 0.85rem;
        }

        .inventory-table th,
        .inventory-table td {
          padding: 0.75rem 0.5rem;
        }

        .inventory-stats {
          grid-template-columns: repeat(2, 1fr);
        }
      }
    `,
  ],
})
export class InventoryMonitorComponent implements OnInit {
  inventoryItems = signal<InventoryItem[]>([]);
  isLoading = signal(true);
  lastUpdateTime = signal(new Date());

  constructor(private apiService: ApiService, private eventService: EventService) {}

  ngOnInit(): void {
    this.loadInventory();
    // Refresh every 5 seconds
    setInterval(() => this.loadInventory(), 5000);

    // Subscribe to inventory updates from EventService
    this.eventService.inventoryUpdates.subscribe((update) => {
      if (update.items && update.items.length > 0) {
        this.inventoryItems.set(update.items);
        this.lastUpdateTime.set(update.lastUpdate);
      }
    });
  }

  loadInventory(): void {
    this.isLoading.set(true);
    this.apiService.getInventory().subscribe({
      next: (items) => {
        this.inventoryItems.set(items);
        this.lastUpdateTime.set(new Date());
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Erro ao carregar o estoque:', error);
        this.isLoading.set(false);
      },
    });
  }

  refreshInventory(): void {
    this.loadInventory();
  }

  getLastUpdate(): string {
    const now = new Date();
    const diff = Math.floor((now.getTime() - this.lastUpdateTime().getTime()) / 1000);

    if (diff < 60) {
      return `há ${diff}s`;
    } else if (diff < 3600) {
      return `há ${Math.floor(diff / 60)}m`;
    } else {
      return this.lastUpdateTime().toLocaleTimeString();
    }
  }

  getTotalQuantity(): number {
    return this.inventoryItems().reduce((sum, item) => sum + item.quantity, 0);
  }

  getLowStockCount(): number {
    return this.inventoryItems().filter((item) => item.quantity < 10 && item.quantity >= 5).length;
  }

  getCriticalStockCount(): number {
    return this.inventoryItems().filter((item) => item.quantity < 5).length;
  }

  getStatusClass(status: string): string {
    return status.toLowerCase().replace(' ', '-');
  }

  translateStatus(status: string): string {
    const labels: { [key: string]: string } = {
      AVAILABLE: 'Disponível',
      RESERVED: 'Reservado',
      UNAVAILABLE: 'Indisponível',
    };

    return labels[status] || status;
  }
}
