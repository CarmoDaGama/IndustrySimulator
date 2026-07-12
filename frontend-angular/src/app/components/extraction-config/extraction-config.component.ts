import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ExtractionConfig } from '../../models';

/**
 * Portal de Configurações — Camada 1 (Extracção).
 * Define genericamente QUE recursos são extraídos, com que tempos e para que
 * fim (purpose). Sem nenhuma configuração activa aqui, os Workers da Camada 1
 * ficam à espera e a cadeia não arranca.
 */
@Component({
  selector: 'app-extraction-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <h3><i class="material-icons">landscape</i> Recursos Extraídos (Camada 1)</h3>
        <button (click)="load()" class="btn btn-secondary btn-sm">
          <i class="material-icons">refresh</i>
        </button>
      </div>

      <p class="hint">
        A extracção é autónoma e contínua: assim que um recurso estiver activo,
        os Workers da Camada 1 extraem-no em ciclo, sem intervenção manual.
        Os <strong>tempos</strong> vêm da pipeline da Camada 1 (separador Pipelines) —
        sem pipeline, a extracção não arranca.
      </p>

      <div class="list">
        <div *ngIf="configs().length === 0" class="empty-msg">
          Nenhum recurso configurado — a cadeia está parada.
        </div>

        <div *ngFor="let c of configs()" class="row">
          <div class="main">
            <strong>{{ c.materialName }}</strong>
            <span class="type">{{ c.materialType }}</span>
            <span class="badge" [class.on]="c.active">{{ c.active ? 'activo' : 'inactivo' }}</span>
          </div>
          <div class="meta">
            <span><i class="material-icons">flag</i> {{ c.targetProduct }} / {{ c.targetComponent }}</span>
            <span><i class="material-icons">factory</i> {{ c.factory }}</span>
          </div>
          <button class="btn-icon danger" (click)="remove(c)"><i class="material-icons">delete</i></button>
        </div>
      </div>

      <hr />

      <h4>Adicionar recurso</h4>
      <form class="form-grid" (ngSubmit)="add()">
        <label>Material <input name="materialName" [(ngModel)]="draft.materialName" required /></label>
        <label>Tipo <input name="materialType" [(ngModel)]="draft.materialType" required /></label>
        <label>Qtd/ciclo <input type="number" name="quantityPerCycle" [(ngModel)]="draft.quantityPerCycle" /></label>
        <label>Unidade <input name="unit" [(ngModel)]="draft.unit" /></label>
        <label>Fábrica <input name="factory" [(ngModel)]="draft.factory" /></label>
        <label>Produto alvo <input name="targetProduct" [(ngModel)]="draft.targetProduct" placeholder="CAR" /></label>
        <label>Componente alvo <input name="targetComponent" [(ngModel)]="draft.targetComponent" placeholder="ENGINE" /></label>
        <label class="wide">Descrição (purpose) <input name="description" [(ngModel)]="draft.description" /></label>
        <label class="check">
          <input type="checkbox" name="active" [(ngModel)]="draft.active" /> Activo
        </label>
        <button type="submit" class="btn btn-primary" [disabled]="saving()">
          {{ saving() ? 'A guardar...' : 'Adicionar' }}
        </button>
      </form>

      <p class="err" *ngIf="error()">{{ error() }}</p>
    </div>
  `,
  styles: [
    `
      .card { padding: 1.5rem; border-radius: 1rem; }
      .card-header { display: flex; justify-content: space-between; align-items: center; }
      .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
      .hint { font-size: 0.85rem; color: var(--text-muted, #888); margin: 0.5rem 0 1rem; }
      .list { display: flex; flex-direction: column; gap: 0.6rem; }
      .empty-msg { padding: 1rem; text-align: center; color: var(--text-muted, #888); font-style: italic; }
      .row {
        display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: 1rem;
        padding: 0.75rem 1rem; border-radius: 0.75rem; background: rgba(255,255,255,0.04);
      }
      .main { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
      .type { font-size: 0.75rem; opacity: 0.7; }
      .badge { font-size: 0.7rem; padding: 0.15rem 0.5rem; border-radius: 999px; background: rgba(255,255,255,0.1); }
      .badge.on { background: rgba(16,185,129,0.2); color: #10b981; }
      .meta { display: flex; flex-direction: column; gap: 0.2rem; font-size: 0.75rem; color: var(--text-muted, #888); }
      .meta span { display: flex; align-items: center; gap: 0.3rem; }
      .meta i { font-size: 0.9rem; }
      hr { border: none; border-top: 1px solid rgba(255,255,255,0.1); margin: 1.25rem 0; }
      h4 { margin: 0 0 0.75rem; font-size: 0.95rem; }
      .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 0.75rem; align-items: end; }
      .form-grid label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.75rem; font-weight: 600; }
      .form-grid input[type='text'], .form-grid input:not([type]), .form-grid input[type='number'] {
        padding: 0.45rem 0.6rem; border-radius: 6px; border: 1px solid rgba(255,255,255,0.15);
        background: rgba(255,255,255,0.05); color: inherit;
      }
      .form-grid .wide { grid-column: span 2; }
      .form-grid .check { flex-direction: row; align-items: center; gap: 0.4rem; }
      .btn-icon.danger { border: none; background: transparent; color: #e74c3c; cursor: pointer; }
      .err { color: #e74c3c; font-size: 0.8rem; }
    `,
  ],
})
export class ExtractionConfigComponent implements OnInit {
  configs = signal<ExtractionConfig[]>([]);
  saving = signal(false);
  error = signal<string | null>(null);

  draft: ExtractionConfig = this.emptyDraft();

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getExtractionConfigs().subscribe({
      next: (c) => this.configs.set(c || []),
      error: () => this.error.set('Não foi possível carregar as configurações'),
    });
  }

  add(): void {
    if (!this.draft.materialName || !this.draft.materialType) {
      this.error.set('Material e tipo são obrigatórios');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.api.createExtractionConfig(this.draft).subscribe({
      next: () => {
        this.saving.set(false);
        this.draft = this.emptyDraft();
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Falha ao adicionar o recurso');
      },
    });
  }

  remove(config: ExtractionConfig): void {
    if (config.id == null) return;
    this.api.deleteExtractionConfig(config.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Falha ao remover'),
    });
  }

  private emptyDraft(): ExtractionConfig {
    return {
      materialName: '',
      materialType: '',
      quantityPerCycle: 1,
      unit: 'kg',
      factory: 'mining-site-alpha',
      targetProduct: '',
      targetComponent: '',
      description: '',
      active: true,
    };
  }
}
