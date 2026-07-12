import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { PipelineStep, SERVICES, ServiceInfo, ServiceKey } from '../../models';

/**
 * Portal de Configurações — etapas (durationMs) da pipeline de cada
 * microserviço. A Camada 1 não aparece aqui: os seus tempos vivem na
 * configuração de extracção.
 */
@Component({
  selector: 'app-pipeline-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass animate-fade-in">
      <div class="card-header">
        <h3><i class="material-icons">settings_suggest</i> Pipeline de Produção</h3>
        <button (click)="addStep()" class="btn btn-secondary btn-sm">
          <i class="material-icons">add</i>
        </button>
      </div>

      <div class="svc-tabs">
        <button
          *ngFor="let s of services"
          class="svc-tab"
          [class.active]="selected() === s.key"
          (click)="select(s.key)"
        >
          <span class="layer">{{ s.layer }}</span>
          <span>{{ s.label }}</span>
        </button>
      </div>

      <p class="svc-hint" *ngIf="selected() === 'raw-material'">
        Ao definir etapas aqui, elas passam a ditar as fases e os tempos da extracção
        (ex.: EXTRACTION, INITIAL_PROCESSING, PACKAGING_FOR_TRANSPORT). Sem etapas, valem
        os tempos definidos em cada recurso, no separador Configurações.
      </p>

      <div class="steps-container">
        <div *ngIf="steps().length === 0" class="empty-msg">
          Sem etapas — será usada a duração por omissão do serviço.
        </div>

        <div *ngFor="let step of steps(); let i = index" class="step-box">
          <div class="step-head">
            <span class="step-idx">#{{ i + 1 }}</span>
            <input [(ngModel)]="step.stepName" placeholder="Nome da Etapa (ex: SMELTING)" class="input-field minimal" />
            <button (click)="removeStep(i)" class="btn-icon danger"><i class="material-icons">delete</i></button>
          </div>

          <div class="step-body">
            <div class="field">
              <label>Duração (ms)</label>
              <input type="number" [(ngModel)]="step.durationMs" class="input-field minimal" />
            </div>
            <div class="field">
              <label>Descrição</label>
              <input [(ngModel)]="step.description" placeholder="O que ocorre aqui?" class="input-field minimal" />
            </div>
          </div>
        </div>
      </div>

      <div class="card-footer" *ngIf="steps().length > 0">
        <div class="total-info">
          Tempo Total: <strong>{{ totalDuration() }}ms</strong>
        </div>
        <button (click)="savePipeline()" class="btn btn-primary" [disabled]="isSaving()">
          <i class="material-icons">cloud_upload</i> Guardar
        </button>
      </div>

      <div *ngIf="msg()" class="toast" [class.error]="isError()">{{ msg() }}</div>
    </div>
  `,
  styles: [`
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .svc-tabs { display: flex; gap: 0.5rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
    .svc-tab { display: flex; flex-direction: column; align-items: flex-start; gap: 0.1rem; padding: 0.5rem 0.85rem; border-radius: 0.6rem; border: 1px solid var(--border, rgba(255,255,255,0.15)); background: transparent; color: inherit; cursor: pointer; font-size: 0.85rem; }
    .svc-tab .layer { font-size: 0.6rem; text-transform: uppercase; letter-spacing: 0.05em; opacity: 0.6; }
    .svc-tab.active { background: var(--primary, #2563eb); color: #fff; border-color: transparent; }
    .svc-tab.active .layer { opacity: 0.85; }
    .svc-hint { font-size: 0.75rem; color: var(--text-muted); background: rgba(255,255,255,0.04); border-radius: 0.5rem; padding: 0.6rem 0.75rem; margin: 0 0 1rem; line-height: 1.45; }
    .steps-container { display: flex; flex-direction: column; gap: 1rem; max-height: 400px; overflow-y: auto; padding-right: 0.5rem; }
    .step-box { background: rgba(0,0,0,0.2); border-radius: 0.75rem; padding: 1rem; border: 1px solid var(--border); }
    .step-head { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
    .step-idx { background: var(--primary); color: #fff; width: 24px; height: 24px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; font-weight: 800; }
    .step-body { display: grid; grid-template-columns: 100px 1fr; gap: 1rem; }
    .field label { font-size: 0.65rem; text-transform: uppercase; color: var(--text-dim); display: block; margin-bottom: 0.25rem; font-weight: 700; }
    .input-field.minimal { padding: 0.4rem 0.6rem; font-size: 0.85rem; }
    .btn-icon.danger { color: var(--error); background: none; border: none; cursor: pointer; }
    .card-footer { margin-top: 1.5rem; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border); padding-top: 1rem; }
    .total-info { font-size: 0.9rem; color: var(--text-muted); }
    .toast { position: absolute; bottom: 1rem; right: 1rem; padding: 0.75rem 1.5rem; border-radius: 0.5rem; background: var(--success); color: #fff; font-weight: 600; animation: slideUp 0.3s ease-out; }
    .toast.error { background: var(--error); }
    .empty-msg { text-align: center; padding: 2rem; color: var(--text-dim); font-style: italic; }
  `]
})
export class PipelineConfigComponent implements OnInit {
  /** Só as camadas com pipeline configurável (a Camada 1 usa a config de extracção). */
  services: ServiceInfo[] = SERVICES.filter((s) => s.hasPipeline);

  selected = signal<ServiceKey>('processing');
  steps = signal<PipelineStep[]>([]);
  isSaving = signal(false);
  msg = signal('');
  isError = signal(false);

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  select(service: ServiceKey): void {
    if (this.selected() === service) return;
    this.selected.set(service);
    this.steps.set([]);
    this.load();
  }

  private load(): void {
    this.apiService.getPipelineFor(this.selected()).subscribe({
      next: (steps) => this.steps.set(steps || []),
      error: () => this.showMsg('Serviço indisponível', true),
    });
  }

  addStep(): void {
    this.steps.update(s => [...s, { id: s.length + 1, stepName: '', durationMs: 1000, description: '', stepOrder: s.length + 1, isActive: true }]);
  }

  removeStep(i: number): void {
    this.steps.update(s => s.filter((_, idx) => idx !== i));
  }

  totalDuration(): number {
    return this.steps().reduce((acc, s) => acc + Number(s.durationMs), 0);
  }

  savePipeline(): void {
    this.isSaving.set(true);
    this.apiService.savePipelineFor(this.selected(), this.steps()).subscribe({
      next: (s) => {
        this.steps.set(s);
        this.showMsg('Configuração guardada!');
      },
      error: () => this.showMsg('Falha ao guardar', true)
    });
  }

  private showMsg(m: string, err = false) {
    this.msg.set(m);
    this.isError.set(err);
    this.isSaving.set(false);
    setTimeout(() => this.msg.set(''), 3000);
  }
}
