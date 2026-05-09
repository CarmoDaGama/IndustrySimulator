import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { PipelineConfigComponent } from '../pipeline-config/pipeline-config.component';
import { OrderFormComponent } from '../order-form/order-form.component';
import { InventoryMonitorComponent } from '../inventory-monitor/inventory-monitor.component';
import { EventsMonitorComponent } from '../events-monitor/events-monitor.component';
import { ApiService } from '../../services/api.service';
import { EventService } from '../../services/event.service';
import { WebSocketService } from '../../services/websocket.service';

/**
 * DashboardComponent - Central Hub
 * Connects real-time events to statistics and manages global state
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    NgIf, NgFor, DatePipe,
    PipelineConfigComponent,
    OrderFormComponent,
    InventoryMonitorComponent,
    EventsMonitorComponent,
  ],
  template: `
    <div class="dashboard-wrapper">
      <!-- Top Stats Bar -->
      <div class="stats-bar animate-fade-in">
        <div class="stat-card glass">
          <div class="stat-icon"><i class="material-icons">receipt_long</i></div>
          <div class="stat-content">
            <span class="stat-label">Total de Pedidos</span>
            <span class="stat-value">{{ totalOrders() }}</span>
          </div>
        </div>
        
        <div class="stat-card glass highlight-success">
          <div class="stat-icon"><i class="material-icons">check_circle</i></div>
          <div class="stat-content">
            <span class="stat-label">Concluídos</span>
            <span class="stat-value">{{ completedOrders() }}</span>
          </div>
        </div>

        <div class="stat-card glass highlight-warning">
          <div class="stat-icon"><i class="material-icons">pending_actions</i></div>
          <div class="stat-content">
            <span class="stat-label">Em Fila</span>
            <span class="stat-value">{{ pendingOrders() }}</span>
          </div>
        </div>

        <div class="stat-card glass highlight-primary">
          <div class="stat-icon" [class.pulse]="isWsConnected()">
            <i class="material-icons">{{ isWsConnected() ? 'bolt' : 'link_off' }}</i>
          </div>
          <div class="stat-content">
            <span class="stat-label">Kafka Online</span>
            <span class="stat-value">{{ eventCount() }}</span>
          </div>
        </div>
      </div>

      <!-- Main Navigation -->
      <nav class="dashboard-nav glass">
        <button 
          *ngFor="let tab of tabs" 
          (click)="selectTab(tab.id)"
          class="nav-item"
          [class.active]="activeTab() === tab.id"
        >
          <i class="material-icons">{{ tab.icon }}</i>
          <span>{{ tab.label }}</span>
        </button>
      </nav>

      <!-- Content Area -->
      <div class="dashboard-content">
        <!-- Configuration -->
        <div *ngIf="activeTab() === 'configuration'" class="pane animate-slide-up">
          <div class="grid-2">
            <app-pipeline-config></app-pipeline-config>
            <app-order-form></app-order-form>
          </div>
        </div>

        <!-- Monitoring -->
        <div *ngIf="activeTab() === 'monitoring'" class="pane animate-slide-up">
          <div class="grid-layout">
            <div class="col-main">
              <app-events-monitor></app-events-monitor>
            </div>
            <div class="col-side">
              <app-inventory-monitor></app-inventory-monitor>
              
              <!-- System Health Card -->
              <div class="health-card glass mt-3">
                <h3><i class="material-icons">health_and_safety</i> Integridade</h3>
                <div class="health-item">
                  <span class="label">WebSocket Status:</span>
                  <span class="value" [class.text-success]="isWsConnected()">
                    {{ isWsConnected() ? 'Conectado' : 'Reconectando...' }}
                  </span>
                </div>
                <div class="health-item">
                  <span class="label">Uptime do Sistema:</span>
                  <span class="value">{{ systemUptime() }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Analytics -->
        <div *ngIf="activeTab() === 'analytics'" class="pane animate-slide-up">
           <div class="placeholder-card glass">
              <i class="material-icons">analytics</i>
              <h3>Módulo de Analytics em v2.1</h3>
              <p>O monitoramento avançado de latência e gargalos está em desenvolvimento.</p>
           </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-wrapper { display: flex; flex-direction: column; gap: 2rem; }
    .stats-bar { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; }
    .stat-card { padding: 1.5rem; border-radius: 1rem; display: flex; align-items: center; gap: 1.5rem; transition: var(--transition); }
    .stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.05); color: var(--text-muted); }
    .stat-icon i { font-size: 2rem; }
    .stat-icon.pulse { animation: pulse-icon 2s infinite; }
    @keyframes pulse-icon { 0% { transform: scale(1); } 50% { transform: scale(1.1); filter: brightness(1.3); } 100% { transform: scale(1); } }
    
    .stat-content { display: flex; flex-direction: column; }
    .stat-label { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .stat-value { font-size: 2rem; font-weight: 800; color: #fff; line-height: 1.2; }
    .highlight-success .stat-icon { color: var(--success); background: rgba(16, 185, 129, 0.1); }
    .highlight-warning .stat-icon { color: var(--warning); background: rgba(245, 158, 11, 0.1); }
    .highlight-primary .stat-icon { color: var(--primary-light); background: rgba(99, 102, 241, 0.1); }

    .dashboard-nav { display: flex; padding: 0.5rem; border-radius: 1rem; gap: 0.5rem; width: fit-content; margin: 0 auto; }
    .nav-item { padding: 0.75rem 1.5rem; border-radius: 0.75rem; border: none; background: transparent; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 0.75rem; font-weight: 600; transition: var(--transition); }
    .nav-item:hover { background: rgba(255, 255, 255, 0.05); color: #fff; }
    .nav-item.active { background: var(--primary); color: #fff; box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3); }

    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
    .grid-layout { display: grid; grid-template-columns: 1fr 380px; gap: 2rem; }
    .health-card { padding: 1.5rem; border-radius: 1rem; }
    .health-card h3 { font-size: 1rem; margin-bottom: 1.25rem; display: flex; align-items: center; gap: 0.5rem; color: var(--primary-light); }
    .health-item { display: flex; justify-content: space-between; margin-bottom: 0.75rem; font-size: 0.9rem; }
    .health-item .label { color: var(--text-muted); }
    .health-item .value { font-weight: 700; color: #fff; }
    .text-success { color: var(--success) !important; }

    .placeholder-card { height: 400px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 2rem; text-align: center; padding: 3rem; }
    .placeholder-card i { font-size: 5rem; color: var(--text-dim); margin-bottom: 2rem; }

    @media (max-width: 1200px) { .grid-2, .grid-layout { grid-template-columns: 1fr; } .col-side { order: -1; } }
  `]
})
export class DashboardComponent implements OnInit {
  activeTab = signal('configuration');
  
  totalOrders = signal(0);
  completedOrders = signal(0);
  pendingOrders = signal(0);
  eventCount = signal(0);
  isWsConnected = signal(false);
  
  tabs = [
    { id: 'configuration', label: 'Operações', icon: 'precision_manufacturing' },
    { id: 'monitoring', label: 'Live Monitor', icon: 'sensors' },
    { id: 'analytics', label: 'Estatísticas', icon: 'insights' },
  ];

  private startTime = new Date();

  constructor(
    private apiService: ApiService,
    private eventService: EventService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    // Explicitly connect to WebSockets
    this.wsService.connect();
    
    this.refreshStats();
    setInterval(() => this.refreshStats(), 5000);
    
    // Track Kafka events
    this.eventService.kafkaEvents.subscribe(events => {
      this.eventCount.set(events.length);
    });

    // Track Connection status
    this.wsService.isConnected$.subscribe(status => {
      this.isWsConnected.set(status);
    });
  }

  private refreshStats(): void {
    this.apiService.getMarketOrders().subscribe({
      next: (orders) => {
        this.totalOrders.set(orders.length);
        this.completedOrders.set(orders.filter(o => o.status === 'COMPLETED' || o.status === 'SHIPPED').length);
        this.pendingOrders.set(orders.filter(o => o.status === 'PENDING' || o.status === 'ALLOCATED').length);
      }
    });
  }

  selectTab(tabId: string): void {
    this.activeTab.set(tabId);
  }

  systemUptime(): string {
    const diff = Math.floor((new Date().getTime() - this.startTime.getTime()) / 1000);
    const m = Math.floor(diff / 60);
    const s = diff % 60;
    return `${m}m ${s}s`;
  }
}
