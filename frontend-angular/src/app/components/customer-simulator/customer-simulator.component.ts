import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { CustomerSimulatorConfig } from '../../models';

/**
 * Camada 6 (Mercado) — clientes fictícios que encomendam sozinhos.
 * Cada cliente é uma Thread: "pensa" durante um tempo e depois encomenda um
 * dos produtos que a fábrica sabe montar.
 */
@Component({
  selector: 'app-customer-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <div>
          <h3><i class="material-icons">groups_2</i> Clientes Fictícios</h3>
          <span class="subtitle">Camada 6 — encomendas geradas automaticamente</span>
        </div>
        <span class="state" [class.on]="config.customerCount > 0">
          {{ config.customerCount > 0 ? 'A simular' : 'Parado' }}
        </span>
      </div>

      <p class="hint">
        Cada cliente é uma Thread autónoma. Se o produto pedido não existir em stock, a encomenda
        fica <strong>PENDENTE</strong> e é desbloqueada assim que a produção repuser inventário.
      </p>

      <div class="catalog" *ngIf="catalog().length > 0">
        <span class="cat-label">A fábrica sabe montar:</span>
        <span *ngFor="let p of catalog()" class="chip">{{ p }}</span>
      </div>
      <div class="catalog warn" *ngIf="catalog().length === 0">
        Sem produtos: configure regras de produção na <strong>Camada 4 (Montagem)</strong>,
        senão os clientes não têm o que encomendar.
      </div>

      <div class="form-grid">
        <label>
          Nº de clientes
          <input type="number" min="0" [(ngModel)]="config.customerCount" name="cc" />
        </label>
        <label>
          Tempo entre compras (ms)
          <input type="number" min="500" [(ngModel)]="config.thinkTimeMs" name="tt" />
        </label>
        <label>
          Qtd. mínima
          <input type="number" min="1" [(ngModel)]="config.minQuantity" name="minq" />
        </label>
        <label>
          Qtd. máxima
          <input type="number" min="1" [(ngModel)]="config.maxQuantity" name="maxq" />
        </label>
      </div>

      <button class="btn btn-primary" (click)="apply()" [disabled]="saving()">
        {{ saving() ? 'A aplicar...' : 'Aplicar' }}
      </button>

      <p class="err" *ngIf="error()">{{ error() }}</p>
    </div>
  `,
  styles: [`
    .card { padding: 1.5rem; border-radius: 1rem; min-width: 0; }
    .card-header { display: flex; justify-content: space-between; align-items: flex-start; }
    .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
    .subtitle { font-size: 0.72rem; color: var(--text-dim); }
    .state { font-size: 0.65rem; font-weight: 800; padding: 0.2rem 0.5rem; border-radius: 999px; background: rgba(255,255,255,0.08); color: var(--text-dim); }
    .state.on { background: rgba(16,185,129,0.18); color: var(--primary-light); }
    .hint { font-size: 0.75rem; color: var(--text-muted); margin: 0.75rem 0; line-height: 1.45; }
    .catalog { display: flex; flex-wrap: wrap; align-items: center; gap: 0.4rem; margin-bottom: 1rem; font-size: 0.75rem; }
    .cat-label { color: var(--text-dim); }
    .catalog.warn { color: var(--warning); background: rgba(245,158,11,0.1); padding: 0.6rem 0.75rem; border-radius: 0.5rem; line-height: 1.45; }
    .chip { padding: 0.15rem 0.5rem; border-radius: 999px; background: rgba(16,185,129,0.15); color: var(--primary-light); font-weight: 600; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 0.75rem; margin-bottom: 1rem; }
    .form-grid label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.72rem; font-weight: 600; }
    .form-grid input { padding: 0.45rem 0.6rem; border-radius: 6px; border: 1px solid var(--border); background: rgba(0,0,0,0.35); color: inherit; }
    .err { color: var(--error); font-size: 0.8rem; margin-top: 0.5rem; }
  `]
})
export class CustomerSimulatorComponent implements OnInit {
  config: CustomerSimulatorConfig = {
    customerCount: 0,
    thinkTimeMs: 8000,
    minQuantity: 1,
    maxQuantity: 3,
  };
  catalog = signal<string[]>([]);
  saving = signal(false);
  error = signal<string | null>(null);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getCustomerSimulator().subscribe({
      next: (c) => (this.config = c),
      error: () => this.error.set('Serviço de mercado indisponível'),
    });
    this.api.getProductCatalog().subscribe({
      next: (p) => this.catalog.set(p || []),
      error: () => {},
    });
  }

  apply(): void {
    this.saving.set(true);
    this.error.set(null);
    this.api.updateCustomerSimulator(this.config).subscribe({
      next: (c) => {
        this.config = c;
        this.saving.set(false);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Falha ao aplicar');
      },
    });
  }
}
