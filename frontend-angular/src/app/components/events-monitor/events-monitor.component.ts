import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../services/event.service';
import { KafkaEvent } from '../../models';

/**
 * EventsMonitorComponent
 * Real-time monitoring of Kafka events
 * Displays all events from production pipeline in chronological order
 */
@Component({
  selector: 'app-events-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="events-monitor-container">
      <div class="monitor-header">
        <h2><i class="material-icons">rss_feed</i> Monitor de eventos (Kafka)</h2>
        <p class="description">Fluxo de eventos em tempo real da linha de produção</p>
      </div>

      <div class="monitor-content">
        <!-- Controls -->
        <div class="controls-bar">
          <div class="event-count">
            <span class="badge">{{ getRecentEvents().length }}</span>
            <span>Eventos recentes</span>
          </div>

          <div class="controls">
            <button (click)="clearEvents()" class="btn btn-secondary btn-sm">
              <i class="material-icons">delete</i>
              Limpar eventos
            </button>
            <button (click)="pauseAutoScroll()" class="btn btn-secondary btn-sm">
              <i class="material-icons">{{ isPaused() ? 'play_arrow' : 'pause' }}</i>
              {{ isPaused() ? 'Retomar' : 'Pausar' }}
            </button>
          </div>
        </div>

        <!-- Events Feed -->
        <div class="events-feed" #eventsFeed>
          <div *ngIf="getRecentEvents().length === 0" class="empty-state">
            <p>Aguardando eventos...</p>
          </div>

          <div *ngFor="let event of getRecentEvents()" class="event-item" [class]="getEventClass(event)">
            <div class="event-header">
              <span class="event-type" [class]="'type-' + event.eventType.toLowerCase()">
                {{ formatEventType(event.eventType) }}
              </span>
              <span class="event-time">{{ formatTime(event.timestamp) }}</span>
            </div>

            <div class="event-body">
              <div class="event-detail">
                <span class="label">ID do lote:</span>
                <span class="value">{{ event.batchId }}</span>
              </div>

              <div class="event-detail">
                <span class="label">Status:</span>
                <span class="value status" [class]="'status-' + event.status.toLowerCase()">
                  {{ translateStatus(event.status) }}
                </span>
              </div>

              <div class="event-detail">
                <span class="label">Finalidade:</span>
                <span class="value">{{ translatePurpose(event.purpose) }}</span>
              </div>

              <div *ngIf="event.details" class="event-details-raw">
                <span class="label">Detalhes:</span>
                <pre>{{ event.details | json }}</pre>
              </div>
            </div>

            <div class="event-footer">
              <span class="timestamp">{{ event.timestamp | date : 'HH:mm:ss.SSS' }}</span>
            </div>
          </div>
        </div>

        <!-- Event Statistics -->
        <div class="event-stats">
          <div class="stat-item" *ngFor="let stat of getEventStatistics()">
            <div class="stat-value">{{ stat.count }}</div>
            <div class="stat-label">{{ formatEventType(stat.type) }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .events-monitor-container {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        overflow: hidden;
        display: flex;
        flex-direction: column;
      }

      .monitor-header {
        background: var(--primary-dark);
        color: white;
        padding: 1.5rem;
        border-bottom: 4px solid var(--primary-dark);
      }

      .monitor-header h2 {
        margin: 0 0 0.5rem 0;
        font-size: 1.5rem;
      }

      .description {
        margin: 0;
        opacity: 0.9;
        font-size: 0.9rem;
      }

      .monitor-content {
        padding: 1.5rem;
        display: flex;
        flex-direction: column;
        gap: 1rem;
        flex: 1;
      }

      .controls-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: #eff6ff;
        border-radius: 6px;
      }

      .event-count {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-weight: 600;
        color: #333;
      }

      .badge {
        background: var(--primary-color);
        color: white;
        padding: 0.25rem 0.75rem;
        border-radius: 20px;
        font-size: 0.85rem;
        font-weight: 700;
      }

      .controls {
        display: flex;
        gap: 0.5rem;
      }

      .btn {
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.85rem;
        transition: all 0.3s;
      }

      .btn-secondary {
        background: var(--primary-color);
        color: white;
      }

      .btn-secondary:hover {
        background: var(--primary-dark);
      }

      .btn-sm {
        padding: 0.4rem 0.8rem;
        font-size: 0.8rem;
      }

      .events-feed {
        flex: 1;
        overflow-y: auto;
        border: 1px solid #bfdbfe;
        border-radius: 6px;
        padding: 1rem;
        background: #f8fbff;
        max-height: 500px;
        display: flex;
        flex-direction: column-reverse;
      }

        .empty-state {
          text-align: center;
          padding: 3rem 0;
          color: #64748b;
        }

      .event-item {
        background: white;
        border-left: 4px solid var(--primary-color);
        border-radius: 4px;
        padding: 1rem;
        margin-bottom: 0.75rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        transition: all 0.3s;
      }

      .event-item:hover {
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
      }

      .event-item.error {
        border-left-color: #1e40af;
        background: #eff6ff;
      }

      .event-item.success {
        border-left-color: #2563eb;
        background: #eff6ff;
      }

      .event-item.warning {
        border-left-color: #3b82f6;
        background: #eff6ff;
      }

      .event-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }

      .event-type {
        display: inline-block;
        padding: 0.25rem 0.75rem;
        border-radius: 20px;
        font-size: 0.85rem;
        font-weight: 700;
        color: white;
      }

      .event-type.type-rawmaterialproduced {
        background: #2563eb;
      }

      .event-type.type-processingcompleted {
        background: #1d4ed8;
      }

      .event-type.type-componentassembled {
        background: #3b82f6;
      }

      .event-type.type-productassembled {
        background: #60a5fa;
      }

      .event-type.type-inventoryupdated {
        background: #93c5fd;
      }

      .event-time {
        font-size: 0.85rem;
        color: #999;
      }

      .event-body {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 0.75rem;
        margin-bottom: 0.75rem;
      }

      .event-detail {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }

      .event-detail .label {
        font-weight: 600;
        font-size: 0.85rem;
        color: #475569;
      }

      .event-detail .value {
        font-family: monospace;
        font-size: 0.9rem;
        color: #333;
      }

      .event-detail .status {
        display: inline-block;
        padding: 0.2rem 0.5rem;
        border-radius: 3px;
        font-size: 0.8rem;
      }

      .status-completed {
        background: #dbeafe;
        color: #1d4ed8;
      }

      .status-pending {
        background: #eff6ff;
        color: #2563eb;
      }

      .status-failed {
        background: #dbeafe;
        color: #1e40af;
      }

      .event-details-raw {
        grid-column: 1 / -1;
        margin-top: 0.5rem;
      }

      .event-details-raw .label {
        display: block;
        margin-bottom: 0.5rem;
      }

      .event-details-raw pre {
        background: #eff6ff;
        padding: 0.5rem;
        border-radius: 3px;
        font-size: 0.75rem;
        overflow-x: auto;
        margin: 0;
      }

      .event-footer {
        font-size: 0.8rem;
        color: #999;
        border-top: 1px solid #f0f0f0;
        padding-top: 0.5rem;
      }

      .timestamp {
        font-family: monospace;
      }

      .event-stats {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 1rem;
        padding-top: 1rem;
        border-top: 1px solid #bfdbfe;
      }

      .stat-item {
        text-align: center;
        padding: 1rem;
        background: #eff6ff;
        border-radius: 6px;
      }

      .stat-value {
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--primary-color);
      }

      .stat-label {
        font-size: 0.85rem;
        color: #666;
        margin-top: 0.25rem;
      }

      @media (max-width: 768px) {
        .controls-bar {
          flex-direction: column;
          gap: 1rem;
          align-items: flex-start;
        }

        .controls {
          width: 100%;
          flex-wrap: wrap;
        }

        .event-body {
          grid-template-columns: 1fr;
        }

        .events-feed {
          max-height: 300px;
        }
      }
    `,
  ],
})
export class EventsMonitorComponent implements OnInit {
  isPaused = signal(false);

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    // Component subscribes to events via EventService
  }

  formatEventType(eventType: string): string {
    const labels: { [key: string]: string } = {
      RawMaterialProduced: 'Matéria-prima produzida',
      ProcessingCompleted: 'Processamento concluído',
      ComponentAssembled: 'Componente montado',
      ProductAssembled: 'Produto montado',
      InventoryUpdated: 'Estoque atualizado',
    };

    return labels[eventType] || eventType;
  }

  translateStatus(status: string): string {
    const normalizedStatus = status.toUpperCase();
    const labels: { [key: string]: string } = {
      COMPLETED: 'Concluído',
      PENDING: 'Pendente',
      FAILED: 'Falhou',
      RUNNING: 'Em andamento',
      SUCCESS: 'Concluído',
      ERROR: 'Falhou',
      AVAILABLE: 'Disponível',
      RESERVED: 'Reservado',
      UNAVAILABLE: 'Indisponível',
    };

    return labels[normalizedStatus] || status;
  }

  translatePurpose(purpose: string): string {
    const normalizedPurpose = purpose.toLowerCase();
    const labels: { [key: string]: string } = {
      monitoring: 'Monitoramento',
      production: 'Produção',
      inventory: 'Estoque',
      order: 'Pedido',
    };

    return labels[normalizedPurpose] || purpose;
  }

  getRecentEvents(): KafkaEvent[] {
    return this.eventService.getRecentEvents(50);
  }

  clearEvents(): void {
    this.eventService.clearEvents();
  }

  pauseAutoScroll(): void {
    this.isPaused.update((v) => !v);
  }

  formatTime(timestamp: Date): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60) {
      return `há ${diff}s`;
    } else if (diff < 3600) {
      return `há ${Math.floor(diff / 60)}min`;
    } else {
      return date.toLocaleTimeString();
    }
  }

  getEventClass(event: KafkaEvent): string {
    if (event.status === 'FAILED') {
      return 'error';
    } else if (event.status === 'COMPLETED') {
      return 'success';
    } else {
      return 'warning';
    }
  }

  getEventStatistics(): Array<{ type: string; count: number }> {
    const events = this.getRecentEvents();
    const stats: { [key: string]: number } = {};

    events.forEach((event) => {
      stats[event.eventType] = (stats[event.eventType] || 0) + 1;
    });

    return Object.entries(stats)
      .map(([type, count]) => ({ type, count }))
      .sort((a, b) => b.count - a.count);
  }
}
