import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ProductionRule, ProductionRuleInput, SERVICES, ServiceInfo, ServiceKey } from '../../models';

/**
 * Portal de Configurações — Mapeamento Genérico (Secção 6.3 do enunciado):
 * regras de transformação e árvore de componentes (BOM).
 *
 * Como uma regra aceita vários insumos, a mesma UI serve para:
 *   Ferro ×2 → Aço ×1                          (transformação)
 *   Motor ×1 + Pneus ×4 + Chassis ×1 → Carro   (árvore BOM)
 */
@Component({
  selector: 'app-production-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass">
      <div class="card-header">
        <div>
          <h3><i class="material-icons">account_tree</i> Regras de Produção / BOM</h3>
          <span class="subtitle">O que cada camada consome e o que produz</span>
        </div>
        <button (click)="load()" class="btn btn-secondary btn-sm"><i class="material-icons">refresh</i></button>
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

      <p class="hint">
        Sem regras, esta camada fica <strong>bloqueada</strong> — a produção só arranca quando
        houver configuração. Cada regra tem de consumir, no mínimo, 2 unidades da camada anterior.
      </p>

      <!-- Regras existentes -->
      <div class="rules">
        <div *ngIf="rules().length === 0" class="empty">Nenhuma regra configurada nesta camada.</div>

        <div *ngFor="let r of rules()" class="rule">
          <div class="formula">
            <span class="ins">
              <span *ngFor="let i of r.inputs; let last = last" class="ing">
                {{ i.inputMaterial }} <em>×{{ i.inputQuantity }}</em>
                <span *ngIf="!last" class="plus">+</span>
              </span>
            </span>
            <i class="material-icons arrow">east</i>
            <span class="out">{{ r.outputMaterial }} <em>×{{ r.outputQuantity }}</em></span>
          </div>
          <div class="rule-meta">
            <span *ngIf="r.targetProduct">para {{ r.targetComponent || '—' }} de {{ r.targetProduct }}</span>
            <span *ngIf="r.factory"><i class="material-icons">factory</i> {{ r.factory }}</span>
          </div>
          <button class="btn-icon danger" (click)="remove(r)"><i class="material-icons">delete</i></button>
        </div>
      </div>

      <hr />

      <!-- Nova regra -->
      <h4>Nova regra</h4>

      <div class="inputs-editor">
        <div *ngFor="let inp of draft.inputs; let i = index" class="input-row">
          <input [(ngModel)]="inp.inputMaterial" [name]="'mat' + i" placeholder="Material consumido (ex: Ferro)" class="input-field minimal grow" />
          <span class="times">×</span>
          <input type="number" min="1" [(ngModel)]="inp.inputQuantity" [name]="'qty' + i" class="input-field minimal qty" />
          <button class="btn-icon danger" (click)="removeInput(i)" [disabled]="draft.inputs.length === 1">
            <i class="material-icons">close</i>
          </button>
        </div>
        <button class="btn btn-secondary btn-sm" (click)="addInput()">
          <i class="material-icons">add</i> Adicionar insumo (para BOM)
        </button>
      </div>

      <div class="form-grid">
        <label>Produz <input [(ngModel)]="draft.outputMaterial" name="out" placeholder="ex: Aço" /></label>
        <label>Quantidade <input type="number" min="1" [(ngModel)]="draft.outputQuantity" name="outQty" /></label>
        <label>Fábrica <input [(ngModel)]="draft.factory" name="fab" placeholder="ex: siderurgia-luanda" /></label>
        <label>Produto alvo <input [(ngModel)]="draft.targetProduct" name="tp" placeholder="CAR" /></label>
        <label>Componente alvo <input [(ngModel)]="draft.targetComponent" name="tc" placeholder="ENGINE" /></label>
        <label class="wide">Descrição (purpose) <input [(ngModel)]="draft.description" name="desc" /></label>
      </div>

      <button class="btn btn-primary" (click)="add()" [disabled]="saving()">
        {{ saving() ? 'A guardar...' : 'Criar regra' }}
      </button>

      <p class="err" *ngIf="error()">{{ error() }}</p>
    </div>
  `,
  styles: [`
    .card { padding: 1.5rem; border-radius: 1rem; min-width: 0; overflow: hidden; }
    .card-header { display: flex; justify-content: space-between; align-items: flex-start; }
    .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
    .subtitle { font-size: 0.72rem; color: var(--text-dim); }
    .svc-tabs { display: flex; gap: 0.5rem; margin: 1rem 0; flex-wrap: wrap; }
    .svc-tab { display: flex; flex-direction: column; align-items: flex-start; padding: 0.5rem 0.85rem; border-radius: 0.6rem; border: 1px solid var(--border); background: transparent; color: inherit; cursor: pointer; font-size: 0.85rem; }
    .svc-tab .layer { font-size: 0.6rem; text-transform: uppercase; opacity: 0.6; }
    .svc-tab.active { background: var(--primary); color: #fff; border-color: transparent; }
    .hint { font-size: 0.75rem; color: var(--text-muted); background: rgba(255,255,255,0.04); border-radius: 0.5rem; padding: 0.6rem 0.75rem; line-height: 1.45; }

    .rules { display: flex; flex-direction: column; gap: 0.6rem; margin-top: 1rem; }
    .empty { color: var(--text-dim); font-style: italic; font-size: 0.85rem; padding: 0.5rem 0; }
    .rule { display: grid; grid-template-columns: minmax(0,1fr) auto auto; align-items: center; gap: 0.75rem; padding: 0.75rem; border-radius: 0.7rem; background: rgba(255,255,255,0.04); }
    .formula { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; min-width: 0; }
    .ing { font-size: 0.85rem; }
    .ing em { font-style: normal; font-weight: 800; color: var(--primary-light); }
    .plus { margin: 0 0.35rem; opacity: 0.5; }
    .arrow { opacity: 0.5; font-size: 1rem; }
    .out { font-weight: 700; font-size: 0.9rem; color: #fff; }
    .out em { font-style: normal; color: var(--primary-light); }
    .rule-meta { display: flex; flex-direction: column; gap: 0.15rem; font-size: 0.68rem; color: var(--text-dim); }
    .rule-meta i { font-size: 0.8rem; vertical-align: middle; }

    hr { border: none; border-top: 1px solid var(--border); margin: 1.25rem 0; }
    h4 { margin: 0 0 0.75rem; font-size: 0.95rem; }
    .inputs-editor { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
    .input-row { display: flex; align-items: center; gap: 0.4rem; }
    .grow { flex: 1; min-width: 0; }
    .qty { width: 70px; }
    .times { opacity: 0.6; }
    .btn-icon { background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; }
    .btn-icon.danger { color: var(--error); }
    .btn-icon:disabled { opacity: 0.3; cursor: not-allowed; }

    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 0.75rem; margin-bottom: 1rem; }
    .form-grid label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.72rem; font-weight: 600; }
    .form-grid .wide { grid-column: span 2; }
    .form-grid input, .input-field.minimal { padding: 0.45rem 0.6rem; border-radius: 6px; border: 1px solid var(--border); background: rgba(0,0,0,0.35); color: inherit; }
    .err { color: var(--error); font-size: 0.8rem; margin-top: 0.5rem; }
  `]
})
export class ProductionRulesComponent implements OnInit {
  /** A Camada 1 não transforma nada: extrai. Não tem regras de produção. */
  services: ServiceInfo[] = SERVICES.filter((s) => s.key !== 'raw-material');

  selected = signal<ServiceKey>('processing');
  rules = signal<ProductionRule[]>([]);
  saving = signal(false);
  error = signal<string | null>(null);

  draft: ProductionRule = this.emptyDraft();

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  select(service: ServiceKey): void {
    if (this.selected() === service) return;
    this.selected.set(service);
    this.rules.set([]);
    this.draft = this.emptyDraft();
    this.load();
  }

  load(): void {
    this.api.getProductionRules(this.selected()).subscribe({
      next: (r) => this.rules.set(r || []),
      error: () => this.error.set('Não foi possível carregar as regras'),
    });
  }

  addInput(): void {
    this.draft.inputs.push({ inputMaterial: '', inputQuantity: 2 });
  }

  removeInput(i: number): void {
    if (this.draft.inputs.length > 1) {
      this.draft.inputs.splice(i, 1);
    }
  }

  add(): void {
    const total = this.draft.inputs.reduce((a, i) => a + Number(i.inputQuantity || 0), 0);
    if (this.draft.inputs.some((i) => !i.inputMaterial.trim())) {
      this.error.set('Cada insumo tem de indicar o material.');
      return;
    }
    if (!this.draft.outputMaterial.trim()) {
      this.error.set('Indique o material produzido.');
      return;
    }
    if (total < 2) {
      this.error.set('A regra tem de consumir, no mínimo, 2 unidades da camada anterior.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.api.createProductionRule(this.selected(), this.draft).subscribe({
      next: () => {
        this.saving.set(false);
        this.draft = this.emptyDraft();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(e?.message || 'Falha ao criar a regra');
      },
    });
  }

  remove(rule: ProductionRule): void {
    if (rule.id == null) return;
    this.api.deleteProductionRule(this.selected(), rule.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Falha ao remover'),
    });
  }

  private emptyDraft(): ProductionRule {
    const inputs: ProductionRuleInput[] = [{ inputMaterial: '', inputQuantity: 2 }];
    return {
      outputMaterial: '',
      outputQuantity: 1,
      factory: '',
      targetProduct: '',
      targetComponent: '',
      description: '',
      active: true,
      inputs,
    };
  }
}
