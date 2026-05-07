import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { EventService } from '../../services/event.service';
import { PipelineStep, PipelineConfig } from '../../models';

/**
 * PipelineConfigComponent
 * Allows configuration and management of production pipelines
 * Provides forms to define pipeline steps with durations
 */
@Component({
  selector: 'app-pipeline-config',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="pipeline-config-container">
      <div class="config-header">
        <h2><i class="material-icons">settings</i> Configuração do pipeline</h2>
        <p class="description">Defina as etapas da linha de produção e suas durações</p>
      </div>

      <div class="config-content">
        <!-- Pipeline Selection/Creation -->
        <div class="config-section">
          <h3>Configuração do pipeline</h3>

          <div class="form-group">
            <label>Nome do pipeline:</label>
            <input
              type="text"
              [(ngModel)]="pipelineName"
              placeholder="Informe o nome do pipeline"
              class="input-field"
            />
          </div>

          <div class="form-group">
            <label>Descrição:</label>
            <textarea
              [(ngModel)]="pipelineDescription"
              placeholder="Informe a descrição do pipeline"
              class="input-field"
              rows="2"
            ></textarea>
          </div>
        </div>

        <!-- Pipeline Steps -->
        <div class="config-section">
          <div class="steps-header">
            <h3>Etapas de produção</h3>
            <button (click)="addStep()" class="btn btn-primary btn-sm">
              <i class="material-icons">add</i>
              Adicionar etapa
            </button>
          </div>

          <div class="steps-list">
            <div *ngIf="steps().length === 0" class="empty-state">
              <p>Nenhuma etapa configurada. Clique em "Adicionar etapa" para criar uma.</p>
            </div>

            <div *ngFor="let step of steps(); let i = index" class="step-item">
              <div class="step-number">{{ i + 1 }}</div>

              <div class="step-content">
                <input
                  type="text"
                  [(ngModel)]="step.stepName"
                  placeholder="Nome da etapa"
                  class="step-input"
                />
                <input
                  type="text"
                  [(ngModel)]="step.description"
                  placeholder="Descrição"
                  class="step-input"
                />
                <div class="duration-input">
                  <label>Duração (ms):</label>
                  <input
                    type="number"
                    [(ngModel)]="step.durationMs"
                    min="100"
                    placeholder="1000"
                    class="step-input"
                  />
                </div>
              </div>

              <button (click)="removeStep(i)" class="btn btn-danger btn-sm">
                <i class="material-icons">close</i>
              </button>
            </div>
          </div>

          <!-- Total Duration Display -->
          <div class="total-duration">
            <strong>Duração total do pipeline:</strong>
            <span>{{ calculateTotalDuration() }}ms ({{ (calculateTotalDuration() / 1000).toFixed(1) }}s)</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="config-actions">
          <button (click)="savePipeline()" class="btn btn-success">
            <i class="material-icons">save</i>
            Salvar pipeline
          </button>
          <button (click)="resetPipeline()" class="btn btn-secondary">
            <i class="material-icons">refresh</i>
            Redefinir
          </button>
        </div>

        <!-- Success Message -->
        <div *ngIf="successMessage()" class="message success">
          {{ successMessage() }}
        </div>

        <!-- Error Message -->
        <div *ngIf="errorMessage()" class="message error">
          {{ errorMessage() }}
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .pipeline-config-container {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        overflow: hidden;
      }

      .config-header {
        background: var(--primary-dark);
        color: white;
        padding: 1.5rem;
        border-bottom: 4px solid var(--primary-dark);
      }

      .config-header h2 {
        margin: 0 0 0.5rem 0;
        font-size: 1.5rem;
      }

      .description {
        margin: 0;
        opacity: 0.9;
        font-size: 0.9rem;
      }

      .config-content {
        padding: 2rem;
      }

      .config-section {
        margin-bottom: 2rem;
        padding-bottom: 2rem;
        border-bottom: 1px solid #bfdbfe;
      }

      .config-section:last-of-type {
        border-bottom: none;
      }

      .config-section h3 {
        margin: 0 0 1rem 0;
        color: #1e3a8a;
        font-size: 1.1rem;
      }

      .form-group {
        margin-bottom: 1rem;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 600;
        color: #475569;
      }

      .input-field {
        width: 100%;
        padding: 0.75rem;
        border: 1px solid #bfdbfe;
        border-radius: 4px;
        font-size: 1rem;
        box-sizing: border-box;
        transition: border-color 0.3s;
      }

      .input-field:focus {
        outline: none;
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
      }

      .steps-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .steps-list {
        background: #eff6ff;
        border-radius: 6px;
        padding: 1rem;
        margin-bottom: 1rem;
        min-height: 100px;
      }

      .empty-state {
        text-align: center;
        color: #64748b;
        padding: 2rem;
      }

      .step-item {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;
        background: white;
        padding: 1rem;
        border-radius: 4px;
        border: 1px solid #bfdbfe;
      }

      .step-number {
        min-width: 40px;
        height: 40px;
        border-radius: 50%;
        background: var(--primary-color);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: bold;
        font-size: 1.1rem;
      }

      .step-content {
        flex: 1;
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.75rem;
      }

      .step-input {
        padding: 0.5rem;
        border: 1px solid #bfdbfe;
        border-radius: 4px;
        font-size: 0.9rem;
      }

      .duration-input {
        grid-column: 2;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }

      .duration-input label {
        font-size: 0.8rem;
        color: #475569;
        margin: 0;
      }

      .total-duration {
        background: #eff6ff;
        padding: 1rem;
        border-radius: 4px;
        border-left: 4px solid var(--primary-color);
        display: flex;
        justify-content: space-between;
      }

      .total-duration strong {
        color: #1e3a8a;
      }

      .total-duration span {
        color: var(--primary-dark);
        font-weight: 600;
      }

      .config-actions {
        display: flex;
        gap: 1rem;
        margin-top: 2rem;
      }

      .btn {
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 4px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.3s;
        font-weight: 600;
      }

      .btn-primary {
        background: var(--primary-color);
        color: white;
      }

      .btn-primary:hover {
        background: var(--primary-dark);
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
      }

      .btn-success {
        background: var(--primary-color);
        color: white;
      }

      .btn-success:hover {
        background: var(--primary-dark);
      }

      .btn-secondary {
        background: var(--primary-dark);
        color: white;
      }

      .btn-secondary:hover {
        background: var(--primary-color);
      }

      .btn-danger {
        background: var(--primary-color);
        color: white;
        padding: 0.5rem 1rem;
      }

      .btn-danger:hover {
        background: var(--primary-dark);
      }

      .btn-sm {
        padding: 0.5rem 1rem;
        font-size: 0.9rem;
      }

      .message {
        padding: 1rem;
        border-radius: 4px;
        margin-top: 1rem;
      }

      .message.success {
        background: #dbeafe;
        color: #1d4ed8;
        border-left: 4px solid var(--primary-color);
      }

      .message.error {
        background: #eff6ff;
        color: #1e40af;
        border-left: 4px solid var(--primary-dark);
      }
    `,
  ],
})
export class PipelineConfigComponent implements OnInit {
  pipelineName = '';
  pipelineDescription = '';
  steps = signal<PipelineStep[]>([]);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  constructor(private apiService: ApiService, private eventService: EventService) {}

  ngOnInit(): void {
    this.loadCurrentPipeline();
  }

  private loadCurrentPipeline(): void {
    this.apiService.getPipeline().subscribe({
      next: (steps) => {
        if (steps && steps.length > 0) {
          this.steps.set(steps);
          // Set name/description from first step or defaults if not stored as a separate entity
          this.pipelineName = 'Pipeline de Produção';
        }
      },
      error: (err) => console.error('Erro ao carregar pipeline:', err)
    });
  }

  private initializeDefaultPipeline(): void {
    // Initialize with empty steps
    this.steps.set([]);
  }

  addStep(): void {
    const newStep: PipelineStep = {
      id: this.steps().length + 1,
      stepName: '',
      stepOrder: this.steps().length + 1,
      durationMs: 1000,
      description: '',
      isActive: true,
    };
    this.steps.update((current) => [...current, newStep]);
  }

  removeStep(index: number): void {
    this.steps.update((current) => current.filter((_, i) => i !== index));
  }

  calculateTotalDuration(): number {
    return this.steps().reduce((sum, step) => sum + step.durationMs, 0);
  }

  savePipeline(): void {
    if (!this.pipelineName.trim()) {
      this.errorMessage.set('O nome do pipeline é obrigatório');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return;
    }

    if (this.steps().length === 0) {
      this.errorMessage.set('Adicione pelo menos uma etapa ao pipeline');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return;
    }

    const config: PipelineConfig = {
      name: this.pipelineName,
      description: this.pipelineDescription,
      steps: this.steps(),
      estimatedDuration: this.calculateTotalDuration(),
    };

    console.log('Salvando configuração do pipeline:', config);
    
    this.apiService.savePipeline(this.steps()).subscribe({
      next: (savedSteps) => {
        this.steps.set(savedSteps);
        this.successMessage.set('Configuração do pipeline salva com sucesso!');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (error) => {
        this.errorMessage.set(`Erro ao salvar pipeline: ${error.message}`);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  resetPipeline(): void {
    this.pipelineName = '';
    this.pipelineDescription = '';
    this.steps.set([]);
    this.successMessage.set('');
    this.errorMessage.set('');
  }
}
