import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ProductionRequest } from '../../models';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card glass animate-fade-in">
      <div class="card-header">
        <h3><i class="material-icons">add_shopping_cart</i> Lançar Encomenda</h3>
      </div>

      <form (ngSubmit)="submit()" class="order-form">
        <div class="form-grid">
          <div class="field full">
            <label>Cliente</label>
            <input [(ngModel)]="order.customerName" name="customer" class="input-field" placeholder="Ex: Indústrias Gama" required />
          </div>

          <div class="field">
            <label>Produto</label>
            <select [(ngModel)]="order.productType" name="type" class="input-field">
              <option *ngFor="let p of catalog()" [value]="p">{{ p }}</option>
            </select>
            <span class="small warn" *ngIf="catalog().length === 0">
              A fábrica ainda não sabe montar nada — configure regras de produção na Camada 4.
            </span>
          </div>

          <div class="field">
            <label>Quantidade</label>
            <input type="number" [(ngModel)]="order.quantity" name="qty" class="input-field" min="1" max="100" />
          </div>

          <div class="field">
            <label>Prioridade</label>
            <select [(ngModel)]="order.priority" name="priority" class="input-field">
              <option [value]="1">Baixa</option>
              <option [value]="2">Normal</option>
              <option [value]="3">Alta</option>
              <option [value]="4">Crítica</option>
            </select>
          </div>
          
          <div class="field">
            <label>Versão BOM</label>
            <input [(ngModel)]="order.bomVersion" name="bom" class="input-field" placeholder="v1.0.0" />
          </div>
        </div>

        <button type="submit" class="btn btn-primary full-width" [disabled]="loading()">
          <i class="material-icons">rocket_launch</i> 
          {{ loading() ? 'Iniciando Cadeia de Produção...' : 'Lançar Encomenda e Produzir' }}
        </button>
      </form>

      <!-- Feedback Messages -->
      <div *ngIf="lastId()" class="alert success animate-slide-up">
        <i class="material-icons">check_circle</i>
        <div>
          <p>Encomenda iniciada: <strong>{{ lastId() }}</strong></p>
          <p class="small">Extração de matéria-prima iniciada automaticamente. Aguarde os eventos em 15s.</p>
        </div>
      </div>

      <div *ngIf="errorMsg()" class="alert error animate-slide-up">
        <i class="material-icons">error</i>
        <span>{{ errorMsg() }}</span>
      </div>
    </div>
  `,
  styles: [`
    .order-form { display: flex; flex-direction: column; gap: 1.5rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .field.full { grid-column: 1 / -1; }
    .field label { font-size: 0.7rem; text-transform: uppercase; color: var(--text-dim); display: block; margin-bottom: 0.4rem; font-weight: 800; }
    .full-width { width: 100%; margin-top: 1rem; }
    
    .alert { margin-top: 1.5rem; padding: 1rem; border-radius: 0.75rem; display: flex; align-items: flex-start; gap: 1rem; font-size: 0.9rem; font-weight: 600; border: 1px solid transparent; }
    .alert.success { background: rgba(52, 211, 153, 0.15); color: #6ee7b7; border-color: rgba(52, 211, 153, 0.3); }
    .alert.error { background: rgba(248, 113, 113, 0.15); color: #fca5a5; border-color: rgba(248, 113, 113, 0.3); }
    .small { font-size: 0.75rem; font-weight: 400; margin-top: 0.25rem; opacity: 0.9; }
    
    select.input-field { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='white'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 0.75rem center; background-size: 1rem; }
  `]
})
export class OrderFormComponent implements OnInit {
  order: ProductionRequest = {
    customerName: '',
    productType: '',
    quantity: 1,
    priority: 2,
    bomVersion: 'v1.0.0',
    requiredDeliveryDate: new Date(new Date().getTime() + 7 * 24 * 60 * 60 * 1000)
  };

  loading = signal(false);
  lastId = signal('');
  errorMsg = signal('');

  catalog = signal<string[]>([]);

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.getProductCatalog().subscribe({
      next: (p) => {
        this.catalog.set(p || []);
        if (!this.order.productType && p?.length) {
          this.order.productType = p[0];
        }
      },
      error: () => {},
    });
  }

  submit() {
    if (!this.order.customerName) {
      this.showError('Nome do cliente é obrigatório');
      return;
    }

    this.loading.set(true);
    this.errorMsg.set('');
    this.lastId.set('');

    // Step 1: Create Market Order
    this.apiService.createMarketOrder(this.order).subscribe({
      next: (res) => {
        this.lastId.set(res.orderId);
        
        this.loading.set(false);
        setTimeout(() => this.lastId.set(''), 15000);
      },
      error: (err) => {
        this.showError(err.message || 'Falha ao criar encomenda');
        this.loading.set(false);
      }
    });
  }


  private showError(msg: string) {
    this.errorMsg.set(msg);
    setTimeout(() => this.errorMsg.set(''), 5000);
  }
}
