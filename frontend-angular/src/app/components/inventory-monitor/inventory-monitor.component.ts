import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { InventoryItem } from '../../models';

/**
 * Camada 5 — Stock global. Mostra, por produto, quanto foi produzido,
 * quanto está reservado por encomendas e quanto sobra para novas vendas.
 */
@Component({
  selector: 'app-inventory-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card glass h-full">
      <div class="card-header">
        <div>
          <h3><i class="material-icons">warehouse</i> Armazém Central</h3>
          <span class="subtitle">Camada 5 — stock de produtos acabados</span>
        </div>
        <button (click)="load()" class="btn-icon" [class.spinning]="loading()" title="Actualizar">
          <i class="material-icons">refresh</i>
        </button>
      </div>

      <!-- Resumo -->
      <div class="totals" *ngIf="items().length > 0">
        <div class="total">
          <span class="t-value">{{ totalQuantity() }}</span>
          <span class="t-label">em armazém</span>
        </div>
        <div class="total reserved">
          <span class="t-value">{{ totalReserved() }}</span>
          <span class="t-label">reservado</span>
        </div>
        <div class="total available">
          <span class="t-value">{{ totalAvailable() }}</span>
          <span class="t-label">disponível</span>
        </div>
      </div>

      <div class="inventory-list">
        <div *ngFor="let item of items()" class="inventory-item">
          <div class="item-icon">
            <i class="material-icons">{{ icon(item.productName) }}</i>
          </div>

          <div class="item-info">
            <div class="item-name" [title]="item.productName">{{ displayName(item.productName) }}</div>
            <div class="item-meta">
              <i class="material-icons">place</i> {{ item.location }}
              <span class="sep">·</span>
              actualizado {{ agoLabel(item.lastUpdated) }}
            </div>

            <!-- Barra: reservado vs disponível -->
            <div class="bar" [title]="item.reservedQuantity + ' reservado, ' + item.availableQuantity + ' disponível'">
              <div class="bar-reserved" [style.width.%]="reservedPct(item)"></div>
            </div>
            <div class="split">
              <span class="chip reserved">{{ item.reservedQuantity }} reservado</span>
              <span class="chip available" [class.low]="item.availableQuantity < 5">
                {{ item.availableQuantity }} disponível
              </span>
            </div>
          </div>

          <div class="item-stock">
            <div class="stock-value">{{ item.quantity }}</div>
            <div class="stock-label">unid.</div>
          </div>
        </div>

        <div *ngIf="items().length === 0 && !loading()" class="empty-state">
          <i class="material-icons large-icon">inventory_2</i>
          <p>Armazém vazio</p>
          <span class="small">
            Os produtos aparecem aqui quando a cadeia completa uma montagem.
            São precisas 8 matérias-primas para 1 produto (regra 2:1).
          </span>
        </div>

        <div *ngIf="loading() && items().length === 0" class="loading-state">
          A carregar inventário...
        </div>
      </div>
    </div>
  `,
  styles: [`
    .h-full { height: 100%; }
    .card { padding: 1.5rem; border-radius: 1rem; }
    .card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem; }
    .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
    .subtitle { font-size: 0.72rem; color: var(--text-dim, #888); }
    .btn-icon { background: none; border: none; color: var(--text-muted); cursor: pointer; }
    .btn-icon:hover { color: var(--primary); }
    .btn-icon.spinning i { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

    .totals { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; margin-bottom: 1rem; }
    .total { text-align: center; padding: 0.6rem 0.4rem; border-radius: 0.6rem; background: rgba(255,255,255,0.04); }
    .t-value { display: block; font-size: 1.3rem; font-weight: 800; color: #fff; }
    .t-label { font-size: 0.62rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-dim, #888); }
    .total.reserved .t-value { color: #f59e0b; }
    .total.available .t-value { color: #10b981; }

    .inventory-list { display: flex; flex-direction: column; gap: 0.75rem; max-height: 420px; overflow-y: auto; }
    .inventory-item { background: rgba(255,255,255,0.03); padding: 0.75rem 1rem; border-radius: 0.75rem; display: flex; align-items: center; gap: 1rem; border: 1px solid var(--border); }
    .item-icon { width: 40px; height: 40px; background: rgba(99,102,241,0.1); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--primary-light); flex-shrink: 0; }
    .item-info { flex: 1; min-width: 0; }
    .item-name { font-weight: 700; font-size: 0.9rem; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .item-meta { font-size: 0.68rem; color: var(--text-dim, #888); display: flex; align-items: center; gap: 0.25rem; margin-top: 0.1rem; }
    .item-meta i { font-size: 0.8rem; }
    .sep { opacity: 0.5; margin: 0 0.15rem; }

    .bar { height: 5px; border-radius: 999px; background: #10b981; margin: 0.45rem 0 0.3rem; overflow: hidden; }
    .bar-reserved { height: 100%; background: #f59e0b; }
    .split { display: flex; gap: 0.35rem; }
    .chip { font-size: 0.62rem; padding: 0.1rem 0.4rem; border-radius: 999px; font-weight: 700; }
    .chip.reserved { background: rgba(245,158,11,0.15); color: #f59e0b; }
    .chip.available { background: rgba(16,185,129,0.15); color: #10b981; }
    .chip.available.low { background: rgba(239,68,68,0.15); color: #ef4444; }

    .item-stock { text-align: right; flex-shrink: 0; }
    .stock-value { font-weight: 800; font-size: 1.25rem; color: #fff; }
    .stock-label { font-size: 0.6rem; text-transform: uppercase; color: var(--text-dim); font-weight: 700; }

    .empty-state { text-align: center; padding: 2.5rem 1rem; color: var(--text-muted); display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
    .large-icon { font-size: 3rem; color: var(--text-dim); opacity: 0.3; }
    .loading-state { text-align: center; padding: 2rem; color: var(--text-dim); }
    .small { font-size: 0.72rem; font-weight: 400; opacity: 0.8; line-height: 1.45; max-width: 260px; }
  `]
})
export class InventoryMonitorComponent implements OnInit, OnDestroy {
  items = signal<InventoryItem[]>([]);
  loading = signal(false);

  totalQuantity = computed(() => this.items().reduce((a, i) => a + i.quantity, 0));
  totalReserved = computed(() => this.items().reduce((a, i) => a + i.reservedQuantity, 0));
  totalAvailable = computed(() => this.items().reduce((a, i) => a + i.availableQuantity, 0));

  private timer?: ReturnType<typeof setInterval>;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.load();
    this.timer = setInterval(() => this.load(), 5000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  load(): void {
    this.loading.set(true);
    this.apiService.getInventory().subscribe({
      next: (data) => {
        this.items.set(data || []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** O nome gerado pela cadeia é verboso; mostra a parte que identifica o produto. */
  displayName(name: string): string {
    if (!name) return 'Produto';
    return name
      .replace(/^Product-/, '')
      .replace(/^Industrial Part:\s*/, '')
      .replace(/^Processed\s*/, '');
  }

  reservedPct(item: InventoryItem): number {
    if (!item.quantity) return 0;
    return Math.min(100, (item.reservedQuantity / item.quantity) * 100);
  }

  agoLabel(iso: string): string {
    if (!iso) return '—';
    const secs = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
    if (secs < 60) return `há ${secs}s`;
    const mins = Math.floor(secs / 60);
    if (mins < 60) return `há ${mins} min`;
    return `há ${Math.floor(mins / 60)} h`;
  }

  icon(name: string): string {
    const n = (name || '').toLowerCase();
    if (n.includes('carro') || n.includes('car') || n.includes('sedan') || n.includes('suv')) return 'directions_car';
    if (n.includes('phone') || n.includes('telefone')) return 'smartphone';
    if (n.includes('computer') || n.includes('computador')) return 'computer';
    if (n.includes('ferro') || n.includes('steel') || n.includes('aço')) return 'hardware';
    if (n.includes('areia') || n.includes('vidro') || n.includes('silic')) return 'panorama_fish_eye';
    return 'inventory_2';
  }
}
