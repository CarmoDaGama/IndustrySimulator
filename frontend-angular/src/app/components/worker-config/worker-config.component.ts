import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { SERVICES, ServiceInfo, ServiceKey } from '../../models';

interface WorkerRow extends ServiceInfo {
  workerCount: number;
  queueSize: number | null;
  saving: boolean;
  error: string | null;
}

/**
 * Portal de Configurações — número de Workers (Threads) activos por
 * microserviço. Cada Worker é uma linha de produção: aumentar o número
 * aumenta o paralelismo da camada em runtime, sem reiniciar o serviço.
 */
@Component({
  selector: 'app-worker-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <h3><i class="material-icons">groups</i> Workers por Microserviço</h3>
        <button (click)="refresh()" class="btn btn-secondary btn-sm">
          <i class="material-icons">refresh</i> Actualizar
        </button>
      </div>

      <p class="hint">
        Cada Worker é uma Thread que representa uma linha de produção activa.
        A fila mostra as unidades da camada anterior à espera de serem consumidas.
      </p>

      <div class="rows">
        <div *ngFor="let row of rows()" class="worker-row">
          <div class="svc">
            <span class="layer">{{ row.layer }}</span>
            <strong>{{ row.label }}</strong>
          </div>

          <div class="queue" *ngIf="row.queueSize !== null">
            <i class="material-icons">inbox</i>
            <span>Fila: <strong>{{ row.queueSize }}</strong></span>
          </div>
          <div class="queue muted" *ngIf="row.queueSize === null">
            <span>sem fila (autónomo)</span>
          </div>

          <div class="controls">
            <button class="btn-icon" (click)="bump(row, -1)" [disabled]="row.workerCount <= 0 || row.saving">
              <i class="material-icons">remove</i>
            </button>
            <input type="number" min="0" [(ngModel)]="row.workerCount" class="input-field minimal count" />
            <button class="btn-icon" (click)="bump(row, 1)" [disabled]="row.saving">
              <i class="material-icons">add</i>
            </button>
            <button class="btn btn-primary btn-sm" (click)="apply(row)" [disabled]="row.saving">
              {{ row.saving ? 'A aplicar...' : 'Aplicar' }}
            </button>
          </div>

          <span class="err" *ngIf="row.error">{{ row.error }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .card { padding: 1.5rem; border-radius: 1rem; }
      .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
      .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
      .hint { font-size: 0.85rem; color: var(--text-muted, #888); margin: 0 0 1rem 0; }
      .rows { display: flex; flex-direction: column; gap: 0.75rem; }
      .worker-row {
        display: grid;
        grid-template-columns: 1fr auto auto;
        align-items: center;
        gap: 1rem;
        padding: 0.85rem 1rem;
        border-radius: 0.75rem;
        background: rgba(255, 255, 255, 0.04);
      }
      .svc { display: flex; flex-direction: column; }
      .layer { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted, #888); }
      .queue { display: flex; align-items: center; gap: 0.35rem; font-size: 0.85rem; color: var(--text-muted, #888); }
      .queue i { font-size: 1.1rem; }
      .queue.muted { font-style: italic; opacity: 0.6; }
      .controls { display: flex; align-items: center; gap: 0.4rem; }
      .count { width: 60px; text-align: center; }
      .btn-icon {
        width: 32px; height: 32px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15);
        background: transparent; color: inherit; cursor: pointer; display: flex; align-items: center; justify-content: center;
      }
      .btn-icon:disabled { opacity: 0.4; cursor: not-allowed; }
      .err { grid-column: 1 / -1; color: #e74c3c; font-size: 0.8rem; }
      @media (max-width: 720px) {
        .worker-row { grid-template-columns: 1fr; gap: 0.5rem; }
      }
    `,
  ],
})
export class WorkerConfigComponent implements OnInit, OnDestroy {
  rows = signal<WorkerRow[]>([]);
  private timer?: ReturnType<typeof setInterval>;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.rows.set(
      SERVICES.map((s) => ({ ...s, workerCount: 0, queueSize: null, saving: false, error: null }))
    );
    this.refresh();
    // A fila muda continuamente: refrescar para dar visibilidade do gargalo.
    this.timer = setInterval(() => this.refreshQueues(), 3000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  refresh(): void {
    for (const row of this.rows()) {
      this.api.getWorkers(row.key).subscribe({
        next: (status) => this.patch(row.key, {
          workerCount: status.workerCount,
          queueSize: status.queueSize ?? null,
          error: null,
        }),
        error: () => this.patch(row.key, { error: 'Serviço indisponível' }),
      });
    }
  }

  /** Só actualiza a fila, para não sobrepor o valor que o utilizador está a editar. */
  private refreshQueues(): void {
    for (const row of this.rows()) {
      this.api.getWorkers(row.key).subscribe({
        next: (status) => this.patch(row.key, { queueSize: status.queueSize ?? null }),
        error: () => {},
      });
    }
  }

  bump(row: WorkerRow, delta: number): void {
    this.patch(row.key, { workerCount: Math.max(0, Number(row.workerCount) + delta) });
  }

  apply(row: WorkerRow): void {
    this.patch(row.key, { saving: true, error: null });
    this.api.setWorkers(row.key, Number(row.workerCount)).subscribe({
      next: (status) => this.patch(row.key, { workerCount: status.workerCount, saving: false }),
      error: () => this.patch(row.key, { saving: false, error: 'Falha ao aplicar' }),
    });
  }

  private patch(key: ServiceKey, changes: Partial<WorkerRow>): void {
    this.rows.update((rows) => rows.map((r) => (r.key === key ? { ...r, ...changes } : r)));
  }
}
