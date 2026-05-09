import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { PipelineStep } from '../../models';

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

      <div class="steps-container">
        <div *ngIf="steps().length === 0" class="empty-msg">
          Configure as etapas industriais
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
          <i class="material-icons">cloud_upload</i> Salvar
        </button>
      </div>
      
      <div *ngIf="msg()" class="toast" [class.error]="isError()">{{ msg() }}</div>
    </div>
  `,
  styles: [`
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
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
  steps = signal<PipelineStep[]>([]);
  isSaving = signal(false);
  msg = signal('');
  isError = signal(false);

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.getPipeline().subscribe(steps => this.steps.set(steps || []));
  }

  addStep(): void {
    this.steps.update(s => [...s, { id: s.length + 1, stepName: '', durationMs: 1000, description: '', stepOrder: s.length + 1, isActive: true }]);
  }

  removeStep(i: number): void {
    this.steps.update(s => s.filter((_, idx) => idx !== i));
  }

  totalDuration(): number {
    return this.steps().reduce((acc, s) => acc + s.durationMs, 0);
  }

  savePipeline(): void {
    this.isSaving.set(true);
    this.apiService.savePipeline(this.steps()).subscribe({
      next: (s) => {
        this.steps.set(s);
        this.showMsg('Configuração salva!');
      },
      error: () => this.showMsg('Falha ao salvar', true)
    });
  }

  private showMsg(m: string, err = false) {
    this.msg.set(m);
    this.isError.set(err);
    this.isSaving.set(false);
    setTimeout(() => this.msg.set(''), 3000);
  }
}
