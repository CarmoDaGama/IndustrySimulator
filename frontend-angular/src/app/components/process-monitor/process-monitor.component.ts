import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { SERVICES, ServiceInfo, ServiceKey, WorkerActivity } from '../../models';

interface LayerView extends ServiceInfo {
  workers: WorkerActivity[];
  queueSize: number | null;
  offline: boolean;
}

/**
 * Monitorização de processos: mostra, em tempo real, a etapa que cada Worker
 * (Thread) de cada camada está a executar, com o progresso da etapa. Torna
 * visíveis os dois comportamentos centrais do simulador — a pipeline com
 * tempo e o bloqueio por escassez — bem como os gargalos (fila a crescer).
 */
@Component({
  selector: 'app-process-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <h3><i class="material-icons">precision_manufacturing</i> Processos em Execução</h3>
        <span class="legend">
          <span class="dot running"></span> a produzir
          <span class="dot blocked"></span> bloqueado (sem insumos)
          <span class="dot idle"></span> inactivo
        </span>
      </div>

      <div class="chain">
        <div *ngFor="let layer of layers(); let last = last" class="layer-wrap">
          <div class="layer" [class.offline]="layer.offline">
            <div class="layer-head">
              <div>
                <span class="layer-tag">{{ layer.layer }}</span>
                <strong>{{ layer.label }}</strong>
              </div>
              <span class="queue" *ngIf="layer.queueSize !== null" [class.warn]="layer.queueSize > 4">
                <i class="material-icons">inbox</i> {{ layer.queueSize }}
              </span>
            </div>

            <div *ngIf="layer.offline" class="offline-msg">serviço indisponível</div>

            <div *ngIf="!layer.offline && layer.workers.length === 0" class="offline-msg">
              sem workers activos
            </div>

            <div *ngFor="let w of layer.workers" class="worker" [class]="w.state.toLowerCase()">
              <div class="worker-top">
                <span class="wname">{{ shortName(w.workerName) }}</span>
                <span class="step" *ngIf="w.state === 'RUNNING'">
                  {{ w.currentStep }} <em>({{ w.stepIndex }}/{{ w.totalSteps }})</em>
                </span>
                <span class="step muted" *ngIf="w.state === 'BLOCKED'">à espera de insumos</span>
                <span class="step muted" *ngIf="w.state === 'IDLE'">sem configuração</span>
              </div>

              <div class="bar">
                <div class="fill" [style.width.%]="progress(w)"></div>
              </div>

              <div class="worker-bot" *ngIf="w.state === 'RUNNING'">
                <span>{{ w.stepElapsedMs }} / {{ w.stepDurationMs }} ms</span>
                <span class="batch" *ngIf="w.batchIds" [title]="w.batchIds">
                  lote {{ shortBatch(w.batchIds) }}
                </span>
              </div>
            </div>
          </div>

          <i *ngIf="!last" class="material-icons arrow">east</i>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .card { padding: 1.5rem; border-radius: 1rem; }
      .card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1.25rem; }
      .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
      .legend { display: flex; align-items: center; gap: 0.4rem; font-size: 0.72rem; color: var(--text-muted, #888); }
      .dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; margin-left: 0.5rem; }
      .dot.running { background: #10b981; }
      .dot.blocked { background: #f59e0b; }
      .dot.idle { background: #6b7280; }

      .chain { display: flex; align-items: stretch; gap: 0.5rem; overflow-x: auto; padding-bottom: 0.5rem; }
      .layer-wrap { display: flex; align-items: center; gap: 0.5rem; }
      .arrow { color: var(--text-muted, #888); opacity: 0.5; }

      .layer { min-width: 230px; flex: 1; padding: 0.9rem; border-radius: 0.85rem; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); }
      .layer.offline { opacity: 0.45; }
      .layer-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
      .layer-tag { display: block; font-size: 0.6rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted, #888); }
      .queue { display: flex; align-items: center; gap: 0.2rem; font-size: 0.75rem; color: var(--text-muted, #888); }
      .queue i { font-size: 0.95rem; }
      .queue.warn { color: #f59e0b; font-weight: 700; }
      .offline-msg { font-size: 0.75rem; font-style: italic; color: var(--text-muted, #888); padding: 0.5rem 0; }

      .worker { padding: 0.55rem 0.6rem; border-radius: 0.5rem; margin-bottom: 0.5rem; background: rgba(0,0,0,0.22); border-left: 3px solid #6b7280; }
      .worker.running { border-left-color: #10b981; }
      .worker.blocked { border-left-color: #f59e0b; }
      .worker-top { display: flex; justify-content: space-between; align-items: baseline; gap: 0.4rem; }
      .wname { font-size: 0.68rem; color: var(--text-muted, #888); font-family: monospace; }
      .step { font-size: 0.72rem; font-weight: 700; text-align: right; }
      .step em { font-style: normal; opacity: 0.6; font-weight: 400; }
      .step.muted { font-weight: 400; font-style: italic; opacity: 0.7; }

      .bar { height: 4px; border-radius: 999px; background: rgba(255,255,255,0.08); margin: 0.4rem 0 0.25rem; overflow: hidden; }
      .fill { height: 100%; background: #10b981; transition: width 0.9s linear; }
      .worker.blocked .fill, .worker.idle .fill { background: transparent; }

      .worker-bot { display: flex; justify-content: space-between; font-size: 0.65rem; color: var(--text-muted, #888); font-family: monospace; }
      .batch { opacity: 0.7; }
    `,
  ],
})
export class ProcessMonitorComponent implements OnInit, OnDestroy {
  layers = signal<LayerView[]>([]);
  private timer?: ReturnType<typeof setInterval>;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.layers.set(
      SERVICES.map((s) => ({ ...s, workers: [], queueSize: null, offline: false }))
    );
    this.poll();
    // 1s: rápido o suficiente para ver a barra de progresso avançar.
    this.timer = setInterval(() => this.poll(), 1000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private poll(): void {
    for (const layer of this.layers()) {
      this.api.getWorkerActivity(layer.key).subscribe({
        next: (workers) => this.patch(layer.key, { workers: workers || [], offline: false }),
        error: () => this.patch(layer.key, { offline: true, workers: [] }),
      });

      this.api.getWorkers(layer.key).subscribe({
        next: (status) => this.patch(layer.key, { queueSize: status.queueSize ?? null }),
        error: () => {},
      });
    }
  }

  progress(w: WorkerActivity): number {
    if (w.state !== 'RUNNING' || !w.stepDurationMs) return 0;
    return Math.min(100, (w.stepElapsedMs / w.stepDurationMs) * 100);
  }

  /** "processing-worker-0" -> "worker-0" (o prefixo já está no cabeçalho da camada). */
  shortName(name: string): string {
    const i = name.indexOf('worker-');
    return i >= 0 ? name.substring(i) : name;
  }

  shortBatch(batchIds: string): string {
    return batchIds
      .split(',')
      .map((b) => b.substring(0, 8))
      .join(' + ');
  }

  private patch(key: ServiceKey, changes: Partial<LayerView>): void {
    this.layers.update((ls) => ls.map((l) => (l.key === key ? { ...l, ...changes } : l)));
  }
}
