import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../services/event.service';
import { KafkaEvent } from '../../models';
import { TreeViewerComponent } from '../tree-viewer/tree-viewer.component';

/**
 * EventsMonitorComponent - Real-time Kafka Event Feed
 * Displays the latest 50 events from the manufacturing process
 */
@Component({
  selector: 'app-events-monitor',
  standalone: true,
  imports: [CommonModule, TreeViewerComponent],
  template: `
    <div class="card glass animate-fade-in">
      <div class="card-header">
        <div class="title-group">
          <h3><i class="material-icons">bolt</i> Fluxo de Eventos Kafka</h3>
          <span class="live-indicator"><span class="dot"></span> LIVE</span>
        </div>
        <button (click)="clear()" class="btn-text">Limpar</button>
      </div>

      <div class="events-list">
        <!-- Event Item -->
        <div *ngFor="let event of events()" class="event-item" [class.new]="isNew(event)">
          <div class="event-time">{{ event.timestamp | date:'HH:mm:ss' }}</div>
          
          <div class="event-main">
            <div class="event-header">
              <span class="status-badge" [ngClass]="getBadgeClass(event.eventType)">
                {{ event.eventType }}
              </span>
              <span class="batch-id">Batch: {{ event.batchId }}</span>
            </div>
            
            <div class="event-body">
              <p class="purpose">{{ event.purpose }}</p>
              
              <!-- Visualization for v2 Product Tree -->
              <div *ngIf="isProductEvent(event)" class="tree-container">
                <app-tree-viewer [node]="event.details"></app-tree-viewer>
              </div>

              <!-- Generic details -->
              <div *ngIf="!isProductEvent(event)" class="generic-details">
                <pre>{{ event.details | json }}</pre>
              </div>
            </div>

            <div class="event-footer">
              <button (click)="toggleRaw(event.eventId)" class="btn-link">
                {{ showRaw[event.eventId] ? 'Ocultar JSON' : 'Ver JSON Bruto' }}
              </button>
            </div>
            
            <pre *ngIf="showRaw[event.eventId]" class="raw-json">{{ event | json }}</pre>
          </div>
        </div>

        <!-- Empty State -->
        <div *ngIf="events().length === 0" class="empty-state">
          <div class="spinner-pulse"></div>
          <p>Aguardando eventos do Kafka...</p>
          <span class="delay-notice">Dica: Após lançar uma ordem, a extração de matéria-prima leva aproximadamente 15 segundos para gerar o primeiro evento.</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .title-group { display: flex; align-items: center; gap: 1rem; }
    .live-indicator { font-size: 0.65rem; font-weight: 800; color: var(--success); display: flex; align-items: center; gap: 0.4rem; background: rgba(52, 211, 153, 0.1); padding: 0.2rem 0.5rem; border-radius: 4px; }
    .dot { width: 6px; height: 6px; background: var(--success); border-radius: 50%; display: inline-block; animation: blink 1s infinite; }
    @keyframes blink { 0% { opacity: 1; } 50% { opacity: 0.3; } 100% { opacity: 1; } }

    .events-list { display: flex; flex-direction: column; gap: 1rem; max-height: 600px; overflow-y: auto; padding-right: 0.5rem; }
    .event-item { display: flex; gap: 1rem; padding: 1rem; border-radius: 0.75rem; background: rgba(255,255,255,0.03); border: 1px solid var(--border); transition: var(--transition); }
    .event-item.new { border-color: var(--primary); background: rgba(99, 102, 241, 0.05); }
    .event-time { font-size: 0.75rem; color: var(--text-dim); font-weight: 600; min-width: 60px; }
    
    .event-main { flex: 1; display: flex; flex-direction: column; gap: 0.75rem; }
    .event-header { display: flex; justify-content: space-between; align-items: center; }
    .batch-id { font-size: 0.7rem; color: var(--text-dim); font-family: monospace; }
    
    .purpose { font-size: 0.85rem; color: var(--text-muted); font-weight: 500; }
    .tree-container { margin-top: 0.5rem; padding: 0.75rem; background: rgba(0,0,0,0.2); border-radius: 0.5rem; }
    .generic-details pre { font-size: 0.75rem; color: var(--text-dim); overflow-x: auto; max-width: 100%; }
    
    .btn-link { background: none; border: none; color: var(--primary-light); font-size: 0.7rem; cursor: pointer; padding: 0; text-decoration: underline; }
    .raw-json { margin-top: 0.5rem; padding: 0.75rem; background: #000; border-radius: 0.4rem; font-size: 0.7rem; color: #6ee7b7; overflow-x: auto; }
    
    .empty-state { text-align: center; padding: 4rem 2rem; color: var(--text-muted); display: flex; flex-direction: column; align-items: center; gap: 1rem; }
    .delay-notice { font-size: 0.75rem; opacity: 0.7; max-width: 300px; line-height: 1.4; }
    .spinner-pulse { width: 40px; height: 40px; border-radius: 50%; background: var(--primary); animation: pulse-wait 2s infinite ease-in-out; opacity: 0.2; }
    @keyframes pulse-wait { 0% { transform: scale(0.8); opacity: 0.2; } 50% { transform: scale(1.2); opacity: 0.4; } 100% { transform: scale(0.8); opacity: 0.2; } }
  `]
})
export class EventsMonitorComponent implements OnInit {
  events = signal<KafkaEvent[]>([]);
  showRaw: { [key: string]: boolean } = {};
  private newEvents = new Set<string>();

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.eventService.kafkaEvents.subscribe(evs => {
      // Track new events for animation
      evs.forEach(e => {
        if (!this.events().find(old => old.eventId === e.eventId)) {
          this.newEvents.add(e.eventId);
          setTimeout(() => this.newEvents.delete(e.eventId), 2000);
        }
      });
      this.events.set(evs);
    });
  }

  isNew(event: KafkaEvent): boolean {
    return this.newEvents.has(event.eventId);
  }

  isProductEvent(event: KafkaEvent): boolean {
    return event.eventType === 'PRODUCT_ASSEMBLED' || event.eventType === 'COMPONENT_ASSEMBLED';
  }

  toggleRaw(id: string): void {
    this.showRaw[id] = !this.showRaw[id];
  }

  getBadgeClass(type: string): string {
    if (type.includes('PRODUCED') || type.includes('EXTRACTED')) return 'status-success';
    if (type.includes('COMPLETED')) return 'status-completed';
    if (type.includes('FAILED')) return 'status-error';
    return 'status-pending';
  }

  clear(): void {
    this.eventService.clearEvents();
  }
}
