import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { InventoryItem } from '../../models';

@Component({
  selector: 'app-inventory-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card glass animate-fade-in h-full">
      <div class="card-header">
        <h3><i class="material-icons">warehouse</i> Armazém Central</h3>
        <button (click)="load()" class="btn-icon" [class.spinning]="loading()"><i class="material-icons">refresh</i></button>
      </div>

      <div class="inventory-list">
        <div *ngFor="let item of items()" class="inventory-item animate-slide-up">
          <div class="item-icon">
            <i class="material-icons">{{ getIcon(item.productType || item.componentType) }}</i>
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.productType || item.componentType }}</div>
            <div class="item-location">{{ item.warehouseLocation }}</div>
          </div>
          <div class="item-stock">
            <div class="stock-value" [class.low]="item.quantity < 5">
              {{ item.quantity }}
            </div>
            <div class="stock-label">{{ item.unit || 'UN' }}</div>
          </div>
        </div>
        
        <div *ngIf="items().length === 0 && !loading()" class="empty-state">
           <i class="material-icons large-icon">inventory_2</i>
           <p>Nenhum item em estoque</p>
           <span class="small">Inicie uma ordem para produzir itens</span>
        </div>
        
        <div *ngIf="loading() && items().length === 0" class="loading-state">
          Carregando inventário...
        </div>
      </div>
    </div>
  `,
  styles: [`
    .h-full { height: 100%; }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .btn-icon { background: none; border: none; color: var(--text-muted); cursor: pointer; transition: var(--transition); }
    .btn-icon:hover { color: var(--primary); }
    .btn-icon.spinning i { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

    .inventory-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .inventory-item { background: rgba(255,255,255,0.03); padding: 0.75rem 1rem; border-radius: 0.75rem; display: flex; align-items: center; gap: 1rem; border: 1px solid var(--border); transition: var(--transition); }
    .inventory-item:hover { border-color: var(--primary-light); background: rgba(255,255,255,0.05); }
    
    .item-icon { width: 40px; height: 40px; background: rgba(99, 102, 241, 0.1); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--primary-light); }
    .item-info { flex: 1; }
    .item-name { font-weight: 700; font-size: 0.95rem; color: #fff; }
    .item-location { font-size: 0.75rem; color: var(--text-dim); }
    .item-stock { text-align: right; }
    .stock-value { font-weight: 800; font-size: 1.25rem; color: var(--success); }
    .stock-value.low { color: var(--warning); }
    .stock-label { font-size: 0.6rem; text-transform: uppercase; color: var(--text-dim); font-weight: 700; }
    
    .empty-state { text-align: center; padding: 3rem 1rem; color: var(--text-muted); display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
    .large-icon { font-size: 3rem; color: var(--text-dim); opacity: 0.3; }
    .loading-state { text-align: center; padding: 2rem; color: var(--text-dim); }
    .small { font-size: 0.7rem; font-weight: 400; opacity: 0.8; }
  `]
})
export class InventoryMonitorComponent implements OnInit {
  items = signal<InventoryItem[]>([]);
  loading = signal(false);

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.load();
    // Faster polling for better UX during simulation (5s)
    setInterval(() => this.load(), 5000);
  }

  load() {
    this.loading.set(true);
    this.apiService.getInventory().subscribe({
      next: (data) => {
        this.items.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getIcon(name: string): string {
    if (!name) return 'inventory_2';
    const n = name.toLowerCase();
    if (n.includes('car') || n.includes('sedan') || n.includes('suv')) return 'directions_car';
    if (n.includes('phone')) return 'smartphone';
    if (n.includes('computer')) return 'computer';
    if (n.includes('steel') || n.includes('metal')) return 'architecture';
    return 'inventory_2';
  }
}
