import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { CompatibleMaterial } from '../../models';

/**
 * Portal de Configurações — Mapeamento Genérico (BOM).
 * As regras de compatibilidade vivem na base de dados, não no código: a
 * Camada 3 valida cada material contra esta lista antes de produzir a peça.
 */
@Component({
  selector: 'app-bom-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <h3><i class="material-icons">account_tree</i> Compatibilidade BOM</h3>
        <button (click)="load()" class="btn btn-secondary btn-sm">
          <i class="material-icons">refresh</i>
        </button>
      </div>

      <p class="hint">
        Tipos de material aceites pela Camada 3 na validação de BOM. Editável em
        runtime — nada está fixo no código.
      </p>

      <div class="chips">
        <div *ngIf="materials().length === 0" class="empty-msg">
          Sem regras — nenhum material será considerado compatível.
        </div>
        <span *ngFor="let m of materials()" class="chip">
          {{ m.materialType }}
          <button class="x" (click)="remove(m)" title="Remover">
            <i class="material-icons">close</i>
          </button>
        </span>
      </div>

      <form class="add-row" (ngSubmit)="add()">
        <input
          name="materialType"
          [(ngModel)]="draft"
          placeholder="ex: steel, silicio, vidro..."
          class="input-field minimal"
        />
        <button type="submit" class="btn btn-primary btn-sm" [disabled]="saving()">
          <i class="material-icons">add</i> Adicionar
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
      .chips { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1rem; }
      .empty-msg { color: var(--text-muted, #888); font-style: italic; font-size: 0.85rem; }
      .chip {
        display: inline-flex; align-items: center; gap: 0.3rem;
        padding: 0.35rem 0.6rem; border-radius: 999px;
        background: rgba(255, 255, 255, 0.08); font-size: 0.85rem;
      }
      .chip .x {
        border: none; background: transparent; color: inherit; cursor: pointer;
        display: flex; align-items: center; opacity: 0.6; padding: 0;
      }
      .chip .x:hover { opacity: 1; color: #e74c3c; }
      .chip .x i { font-size: 0.95rem; }
      .add-row { display: flex; gap: 0.5rem; }
      .add-row input {
        flex: 1; padding: 0.5rem 0.7rem; border-radius: 6px;
        border: 1px solid rgba(255, 255, 255, 0.15);
        background: rgba(255, 255, 255, 0.05); color: inherit;
      }
      .err { color: #e74c3c; font-size: 0.8rem; margin-top: 0.5rem; }
    `,
  ],
})
export class BomConfigComponent implements OnInit {
  materials = signal<CompatibleMaterial[]>([]);
  saving = signal(false);
  error = signal<string | null>(null);
  draft = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getCompatibleMaterials().subscribe({
      next: (m) => this.materials.set(m || []),
      error: () => this.error.set('Não foi possível carregar as regras de BOM'),
    });
  }

  add(): void {
    const materialType = this.draft.trim();
    if (!materialType) return;

    this.saving.set(true);
    this.error.set(null);
    this.api.addCompatibleMaterial({ materialType }).subscribe({
      next: () => {
        this.saving.set(false);
        this.draft = '';
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Falha ao adicionar (talvez já exista)');
      },
    });
  }

  remove(material: CompatibleMaterial): void {
    if (material.id == null) return;
    this.api.deleteCompatibleMaterial(material.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Falha ao remover'),
    });
  }
}
