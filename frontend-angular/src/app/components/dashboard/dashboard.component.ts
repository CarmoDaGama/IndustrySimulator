import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PipelineConfigComponent } from '../pipeline-config/pipeline-config.component';
import { OrderFormComponent } from '../order-form/order-form.component';
import { InventoryMonitorComponent } from '../inventory-monitor/inventory-monitor.component';
import { EventsMonitorComponent } from '../events-monitor/events-monitor.component';
import { ApiService } from '../../services/api.service';

/**
 * DashboardComponent - Main dashboard component
 * Integrates all monitoring and configuration components
 * Provides tabbed interface for different sections
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    PipelineConfigComponent,
    OrderFormComponent,
    InventoryMonitorComponent,
    EventsMonitorComponent,
  ],
  template: `
    <div class="dashboard">
      <!-- Tab Navigation -->
      <div class="tab-navigation">
        <button
          *ngFor="let tab of tabs"
          (click)="selectTab(tab.id)"
          class="tab-button"
          [class.active]="activeTab() === tab.id"
        >
          <i class="material-icons">{{ tab.icon }}</i>
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab Content -->
      <div class="tab-content">
        <!-- Configuration Tab -->
        <div *ngIf="activeTab() === 'configuration'" class="tab-pane active">
          <div class="tab-grid">
            <div class="tab-section">
              <app-pipeline-config></app-pipeline-config>
            </div>
            <div class="tab-section">
              <app-order-form></app-order-form>
            </div>
          </div>
        </div>

        <!-- Monitoring Tab -->
        <div *ngIf="activeTab() === 'monitoring'" class="tab-pane active">
          <div class="tab-grid">
            <div class="tab-section full-width">
              <app-events-monitor></app-events-monitor>
            </div>
            <div class="tab-section">
              <app-inventory-monitor></app-inventory-monitor>
            </div>
          </div>
        </div>

        <!-- Statistics Tab -->
        <div *ngIf="activeTab() === 'statistics'" class="tab-pane active">
          <div class="statistics-container">
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-icon"><i class="material-icons">inventory_2</i></div>
                <div class="stat-info">
                  <div class="stat-value">{{ getTotalOrders() }}</div>
                    <div class="stat-label">Total de pedidos</div>
                </div>
              </div>

              <div class="stat-card">
                <div class="stat-icon"><i class="material-icons">check_circle</i></div>
                <div class="stat-info">
                  <div class="stat-value">{{ getCompletedOrders() }}</div>
                  <div class="stat-label">Pedidos concluídos</div>
                </div>
              </div>

              <div class="stat-card">
                <div class="stat-icon"><i class="material-icons">hourglass_bottom</i></div>
                <div class="stat-info">
                  <div class="stat-value">{{ getPendingOrders() }}</div>
                  <div class="stat-label">Pedidos pendentes</div>
                </div>
              </div>

              <div class="stat-card">
                <div class="stat-icon"><i class="material-icons">rss_feed</i></div>
                <div class="stat-info">
                  <div class="stat-value">{{ getEventCount() }}</div>
                  <div class="stat-label">Eventos processados</div>
                </div>
              </div>
            </div>

            <div class="performance-metrics">
              <h3>Métricas de desempenho</h3>
              <div class="metrics-grid">
                <div class="metric-item">
                  <span class="metric-label">Duração média do pipeline:</span>
                  <span class="metric-value">~45s</span>
                </div>
                <div class="metric-item">
                  <span class="metric-label">Tempo de atividade do sistema:</span>
                  <span class="metric-value">{{ getSystemUptime() }}</span>
                </div>
                <div class="metric-item">
                  <span class="metric-label">Tamanho da fila:</span>
                  <span class="metric-value">{{ getQueueSize() }}</span>
                </div>
                <div class="metric-item">
                  <span class="metric-label">Taxa de processamento:</span>
                  <span class="metric-value">{{ getProcessingRate() }} itens/min</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Help Tab -->
        <div *ngIf="activeTab() === 'help'" class="tab-pane active">
          <div class="help-container">
            <div class="help-section">
              <h3>Guia rápido</h3>

              <div class="help-card">
                <h4>1. Configurar pipeline</h4>
                <p>Acesse a aba <strong>Configuração</strong> e defina a sua linha de produção:</p>
                <ul>
                  <li>Defina o nome e a descrição do pipeline</li>
                  <li>Adicione as etapas de produção com duração em milissegundos</li>
                  <li>Salve a configuração</li>
                </ul>
              </div>

              <div class="help-card">
                <h4>2. Criar pedido de mercado</h4>
                <p>Crie um novo pedido de produção:</p>
                <ul>
                  <li>Informe o nome do cliente e o tipo de produto</li>
                  <li>Especifique a quantidade e a versão da BOM</li>
                  <li>Defina a data de entrega e a prioridade</li>
                  <li>Envie o pedido</li>
                </ul>
              </div>

              <div class="help-card">
                <h4>3. Monitorar produção</h4>
                <p>Acompanhe a produção em tempo real:</p>
                <ul>
                  <li>Visualize os eventos dos tópicos Kafka</li>
                  <li>Monitore os níveis de estoque</li>
                  <li>Confira o status e o progresso dos pedidos</li>
                </ul>
              </div>

              <div class="help-card">
                <h4><i class="material-icons">account_tree</i> Arquitetura do sistema</h4>
                <p>O Simulador Industrial usa:</p>
                <ul>
                  <li><strong>Spring Boot</strong> - Framework de microsserviços</li>
                  <li><strong>Kafka</strong> - Streaming assíncrono de eventos</li>
                  <li><strong>PostgreSQL</strong> - Banco de dados de cada serviço</li>
                  <li><strong>Angular 17</strong> - Painel frontend</li>
                </ul>
              </div>

              <div class="help-card">
                <h4><i class="material-icons">link</i> Microsserviços</h4>
                <ul>
                  <li><code>http://localhost:8081</code> - Serviço de matéria-prima</li>
                  <li><code>http://localhost:8082</code> - Serviço de processamento</li>
                  <li><code>http://localhost:8083</code> - Serviço de componentes</li>
                  <li><code>http://localhost:8084</code> - Serviço de montagem</li>
                </ul>
              </div>

              <div class="help-card">
                <h4><i class="material-icons">rss_feed</i> Tópicos Kafka</h4>
                <ul>
                  <li><code>raw-material-produced</code> - Eventos de matéria-prima</li>
                  <li><code>processing-completed</code> - Resultados do processamento</li>
                  <li><code>component-assembled</code> - Eventos de montagem de componentes</li>
                  <li><code>product-assembled</code> - Eventos do produto final</li>
                  <li><code>inventory-updated</code> - Atualizações de estoque</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .dashboard {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .tab-navigation {
        display: flex;
        gap: 0.5rem;
        background: white;
        border-radius: 8px;
        padding: 0.5rem;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        flex-wrap: wrap;
      }

      .tab-button {
        padding: 0.75rem 1.5rem;
        border: 2px solid transparent;
        border-radius: 4px;
        background: #f0f0f0;
        color: #666;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s;
        font-size: 0.95rem;
      }

      .tab-button:hover {
        background: #e0e0e0;
        transform: translateY(-2px);
      }

      .tab-button.active {
        background: var(--primary-color);
        color: white;
        border-color: var(--primary-dark);
      }

      .tab-content {
        min-height: 500px;
      }

      .tab-pane {
        animation: fadeIn 0.3s ease-in;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .tab-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1.5rem;
      }

      .tab-section {
        animation: slideIn 0.3s ease-in;
      }

      .tab-section.full-width {
        grid-column: 1 / -1;
      }

      @keyframes slideIn {
        from {
          opacity: 0;
          transform: translateX(-20px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }

      .statistics-container {
        display: flex;
        flex-direction: column;
        gap: 2rem;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1.5rem;
      }

      .stat-card {
        background: white;
        padding: 1.5rem;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        display: flex;
        align-items: center;
        gap: 1.5rem;
        transition: all 0.3s;
      }

      .stat-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .stat-icon {
        font-size: 2.5rem;
      }

      .stat-info {
        flex: 1;
      }

      .stat-value {
        font-size: 1.8rem;
        font-weight: 700;
        color: var(--primary-color);
      }

      .stat-label {
        font-size: 0.9rem;
        color: #666;
        margin-top: 0.25rem;
      }

      .performance-metrics {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .performance-metrics h3 {
        margin: 0 0 1.5rem 0;
        color: #333;
        font-size: 1.2rem;
      }

      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
      }

      .metric-item {
        display: flex;
        justify-content: space-between;
        padding: 1rem;
        background: #eff6ff;
        border-radius: 4px;
        border-left: 4px solid var(--primary-color);
      }

      .metric-label {
        font-weight: 600;
        color: #555;
      }

      .metric-value {
        color: var(--primary-color);
        font-weight: 700;
      }

      .help-container {
        background: white;
        border-radius: 8px;
        padding: 2rem;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .help-section {
        max-width: 1000px;
      }

      .help-section h3 {
        margin: 0 0 1.5rem 0;
        color: #333;
        font-size: 1.3rem;
      }

      .help-card {
        background: #eff6ff;
        padding: 1.5rem;
        border-radius: 6px;
        margin-bottom: 1rem;
        border-left: 4px solid var(--primary-color);
      }

      .help-card h4 {
        margin: 0 0 0.75rem 0;
        color: #333;
      }

      .help-card p {
        margin: 0 0 0.75rem 0;
        color: #666;
        line-height: 1.5;
      }

      .help-card ul {
        margin: 0;
        padding-left: 1.5rem;
        color: #666;
      }

      .help-card li {
        margin-bottom: 0.5rem;
        line-height: 1.5;
      }

      .help-card code {
        background: white;
        padding: 0.2rem 0.5rem;
        border-radius: 3px;
        font-family: monospace;
        color: var(--primary-dark);
      }

      @media (max-width: 1024px) {
        .tab-grid {
          grid-template-columns: 1fr;
        }

        .tab-section.full-width {
          grid-column: 1;
        }
      }

      @media (max-width: 768px) {
        .tab-navigation {
          flex-direction: column;
        }

        .tab-button {
          width: 100%;
          text-align: center;
        }

        .stats-grid,
        .metrics-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  activeTab = signal<string>('configuration');

  tabs = [
    { id: 'configuration', label: 'Configuração', icon: 'settings' },
    { id: 'monitoring', label: 'Monitoramento', icon: 'rss_feed' },
    { id: 'statistics', label: 'Estatísticas', icon: 'bar_chart' },
    { id: 'help', label: 'Ajuda', icon: 'help_outline' },
  ];

  private startTime = new Date();

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    // Initialize dashboard
  }

  selectTab(tabId: string): void {
    this.activeTab.set(tabId);
  }

  getTotalOrders(): number {
    return 42; // Mock data
  }

  getCompletedOrders(): number {
    return 28;
  }

  getPendingOrders(): number {
    return 14;
  }

  getEventCount(): number {
    return 256;
  }

  getSystemUptime(): string {
    const now = new Date();
    const diff = Math.floor((now.getTime() - this.startTime.getTime()) / 1000);
    const hours = Math.floor(diff / 3600);
    const minutes = Math.floor((diff % 3600) / 60);
    return `${hours}h ${minutes}m`;
  }

  getQueueSize(): number {
    return 5;
  }

  getProcessingRate(): number {
    return 12;
  }
}
