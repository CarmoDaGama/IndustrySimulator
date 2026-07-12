import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../services/event.service';
import { KafkaEvent } from '../../models';
import { TreeViewerComponent } from '../tree-viewer/tree-viewer.component';

/** Como cada tipo de evento Kafka é apresentado ao utilizador. */
interface EventMeta {
  label: string;
  layer: string;
  icon: string;
  tone: 'raw' | 'processed' | 'component' | 'product' | 'stock';
}

const EVENT_META: Record<string, EventMeta> = {
  RawMaterialProduced: { label: 'Matéria-prima extraída', layer: 'Camada 1', icon: 'landscape', tone: 'raw' },
  ProcessingCompleted: { label: 'Material refinado', layer: 'Camada 2', icon: 'science', tone: 'processed' },
  ComponentAssembled: { label: 'Componente produzido', layer: 'Camada 3', icon: 'memory', tone: 'component' },
  ProductAssembled: { label: 'Produto montado', layer: 'Camada 4', icon: 'inventory', tone: 'product' },
  InventoryUpdated: { label: 'Stock actualizado', layer: 'Camada 5', icon: 'warehouse', tone: 'stock' },
  OrderStatusUpdate: { label: 'Encomenda actualizada', layer: 'Camada 6', icon: 'receipt_long', tone: 'stock' },
};

/**
 * Fluxo de eventos Kafka em tempo real. Cada evento é apresentado como uma
 * frase legível ("o que aconteceu, em que camada, a partir de quê"), com o
 * JSON bruto disponível a pedido para inspecção técnica.
 */
@Component({
  selector: 'app-events-monitor',
  standalone: true,
  imports: [CommonModule, TreeViewerComponent],
  template: `
    <div class="card glass">
      <div class="card-header">
        <div class="title-group">
          <h3><i class="material-icons">bolt</i> Fluxo de Eventos Kafka</h3>
          <span class="live"><span class="dot"></span> AO VIVO</span>
        </div>
        <button (click)="clear()" class="btn-text">Limpar</button>
      </div>

      <p class="hint">
        Cada linha é um evento real publicado no Kafka por uma camada e consumido pela seguinte.
      </p>

      <!-- Filtros por camada -->
      <div class="filters" *ngIf="events().length > 0">
        <button class="f-chip" [class.on]="filter() === null" (click)="setFilter(null)">
          Todos <span class="count">{{ events().length }}</span>
        </button>
        <button
          *ngFor="let t of presentTypes()"
          class="f-chip"
          [class.on]="filter() === t"
          [ngClass]="meta(t).tone"
          (click)="setFilter(t)"
        >
          <i class="material-icons">{{ meta(t).icon }}</i>
          {{ meta(t).label }}
          <span class="count">{{ countOf(t) }}</span>
        </button>
      </div>

      <div class="events-list">
        <div *ngFor="let event of visible()" class="event" [ngClass]="meta(event.eventType).tone" [class.new]="isNew(event)">
          <div class="ev-icon">
            <i class="material-icons">{{ meta(event.eventType).icon }}</i>
          </div>

          <div class="ev-body">
            <div class="ev-top">
              <span class="ev-label">{{ meta(event.eventType).label }}</span>
              <span class="ev-layer">{{ meta(event.eventType).layer }}</span>
              <span class="ev-fail" *ngIf="event.status === 'FAILED'">FALHOU</span>
              <span class="ev-time">{{ event.timestamp | date: 'HH:mm:ss' }}</span>
            </div>

            <div class="ev-summary">{{ summary(event) }}</div>

            <div class="ev-tags">
              <span class="tag" [title]="event.batchId">lote {{ short(event.batchId) }}</span>
              <span class="tag purpose" *ngIf="purposeOf(event) as p">{{ p }}</span>
            </div>

            <!-- Árvore de dependências (contrato de dados do enunciado) -->
            <div *ngIf="treeNode(event) as node" class="tree">
              <div class="tree-caption">Árvore de dependências (o que foi consumido)</div>
              <app-tree-viewer [node]="node"></app-tree-viewer>
            </div>

            <button (click)="toggleRaw(event.eventId)" class="btn-link">
              {{ showRaw[event.eventId] ? 'Ocultar JSON' : 'Ver JSON do evento' }}
            </button>
            <pre *ngIf="showRaw[event.eventId]" class="raw">{{ event.details | json }}</pre>
          </div>
        </div>

        <div *ngIf="events().length === 0" class="empty">
          <div class="pulse"></div>
          <p>À espera de eventos do Kafka...</p>
          <span class="note">
            A extracção é autónoma: configure um recurso em
            <strong>Configurações → Recursos Extraídos</strong> e os eventos começam a fluir.
          </span>
        </div>

        <div *ngIf="events().length > 0 && visible().length === 0" class="empty">
          <p>Nenhum evento deste tipo ainda.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card { padding: 1.5rem; border-radius: 1rem; min-width: 0; overflow: hidden; }
    .card-header { display: flex; justify-content: space-between; align-items: center; }
    .card-header h3 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
    .title-group { display: flex; align-items: center; gap: 0.75rem; }
    .live { font-size: 0.6rem; font-weight: 800; color: var(--success, #10b981); display: flex; align-items: center; gap: 0.35rem; background: rgba(16,185,129,0.12); padding: 0.15rem 0.45rem; border-radius: 4px; }
    .dot { width: 6px; height: 6px; background: currentColor; border-radius: 50%; animation: blink 1s infinite; }
    @keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0.25; } }
    .btn-text { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 0.8rem; }
    .hint { font-size: 0.78rem; color: var(--text-muted, #888); margin: 0.4rem 0 0.9rem; }

    .filters { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
    .f-chip { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.3rem 0.6rem; border-radius: 999px; border: 1px solid var(--border, rgba(255,255,255,0.12)); background: transparent; color: var(--text-muted, #888); cursor: pointer; font-size: 0.72rem; font-weight: 600; }
    .f-chip i { font-size: 0.9rem; }
    .f-chip .count { background: rgba(255,255,255,0.1); border-radius: 999px; padding: 0 0.35rem; font-size: 0.65rem; }
    .f-chip.on { border-color: currentColor; }
    .raw { --tone: #a78bfa; }
    .processed { --tone: #60a5fa; }
    .component { --tone: #f472b6; }
    .product { --tone: #34d399; }
    .stock { --tone: #fbbf24; }
    .f-chip.on { color: var(--tone, #fff); background: rgba(255,255,255,0.08); }

    .events-list { display: flex; flex-direction: column; gap: 0.6rem; max-height: 600px; overflow-y: auto; padding-right: 0.4rem; }
    .event { display: flex; gap: 0.75rem; padding: 0.8rem; border-radius: 0.7rem; background: rgba(255,255,255,0.03); border-left: 3px solid #6b7280; }
    .event.new { animation: slideIn 0.35s ease-out; }
    @keyframes slideIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: none; } }
    .event { border-left-color: var(--tone, #6b7280); }

    .ev-icon { width: 34px; height: 34px; border-radius: 8px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.06); flex-shrink: 0; }
    .ev-icon i { font-size: 1.1rem; }
    .ev-icon { color: var(--tone, #9ca3af); }

    .ev-body { flex: 1; min-width: 0; }
    .ev-top { display: flex; align-items: baseline; gap: 0.5rem; flex-wrap: wrap; }
    .ev-label { font-weight: 700; font-size: 0.88rem; color: #fff; }
    .ev-layer { font-size: 0.6rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-dim, #888); border: 1px solid var(--border, rgba(255,255,255,0.12)); border-radius: 999px; padding: 0 0.35rem; }
    .ev-fail { font-size: 0.6rem; font-weight: 800; color: #ef4444; }
    .ev-time { margin-left: auto; font-size: 0.7rem; color: var(--text-dim, #888); font-family: monospace; }

    .ev-summary { font-size: 0.8rem; color: var(--text-muted, #aaa); margin-top: 0.15rem; overflow-wrap: anywhere; }
    .ev-tags { display: flex; gap: 0.3rem; margin-top: 0.4rem; flex-wrap: wrap; }
    .tag { font-size: 0.62rem; font-family: monospace; overflow-wrap: anywhere; padding: 0.1rem 0.4rem; border-radius: 4px; background: rgba(255,255,255,0.06); color: var(--text-dim, #888); }
    .tag.purpose { font-family: inherit; }

    .tree { margin-top: 0.6rem; padding: 0.6rem; background: rgba(0,0,0,0.25); border-radius: 0.5rem; max-width: 100%; overflow-x: auto; }
    .tree-caption { font-size: 0.62rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-dim, #888); margin-bottom: 0.4rem; }

    .btn-link { background: none; border: none; color: var(--primary-light, #34d399); font-size: 0.68rem; cursor: pointer; padding: 0.4rem 0 0; text-decoration: underline; }
    .raw { margin-top: 0.4rem; padding: 0.6rem; background: #000; border-radius: 0.4rem; font-size: 0.65rem; color: #6ee7b7; max-height: 220px; overflow: auto; max-width: 100%; white-space: pre-wrap; overflow-wrap: anywhere; }

    .empty { text-align: center; padding: 3rem 1rem; color: var(--text-muted); display: flex; flex-direction: column; align-items: center; gap: 0.6rem; }
    .note { font-size: 0.74rem; opacity: 0.75; max-width: 320px; line-height: 1.45; }
    .pulse { width: 36px; height: 36px; border-radius: 50%; background: var(--primary, #10b981); opacity: 0.2; animation: pw 2s infinite ease-in-out; }
    @keyframes pw { 0%,100% { transform: scale(0.8); opacity: 0.2; } 50% { transform: scale(1.15); opacity: 0.4; } }
  `]
})
export class EventsMonitorComponent implements OnInit {
  events = signal<KafkaEvent[]>([]);
  filter = signal<string | null>(null);
  showRaw: { [key: string]: boolean } = {};
  private newEvents = new Set<string>();

  visible = computed(() => {
    const f = this.filter();
    return f ? this.events().filter((e) => e.eventType === f) : this.events();
  });

  /** Tipos que já apareceram, pela ordem da cadeia. */
  presentTypes = computed(() => {
    const order = Object.keys(EVENT_META);
    const seen = new Set(this.events().map((e) => e.eventType));
    return order.filter((t) => seen.has(t));
  });

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.eventService.kafkaEvents.subscribe((evs) => {
      evs.forEach((e) => {
        if (!this.events().find((old) => old.eventId === e.eventId)) {
          this.newEvents.add(e.eventId);
          setTimeout(() => this.newEvents.delete(e.eventId), 2000);
        }
      });
      this.events.set(evs);
    });
  }

  meta(type: string): EventMeta {
    return EVENT_META[type] ?? { label: type, layer: '—', icon: 'bolt', tone: 'stock' };
  }

  countOf(type: string): number {
    return this.events().filter((e) => e.eventType === type).length;
  }

  setFilter(type: string | null): void {
    this.filter.set(type);
  }

  /** Frase legível: o que este evento significa na prática. */
  summary(event: KafkaEvent): string {
    const d: any = event.details || {};
    switch (event.eventType) {
      case 'RawMaterialProduced':
        return `Extraído ${this.qty(d.quantity, d.unit)} de ${d.material?.name ?? 'matéria-prima'} em ${d.material?.producer?.factory ?? 'fábrica'}.`;
      case 'ProcessingCompleted': {
        const consumed = d.processedMaterial?.components?.length ?? 0;
        return `Refinado ${d.processedMaterial?.name ?? 'material'} a partir de ${consumed} unidade(s) da camada anterior (${d.processingDurationMs ?? 0} ms).`;
      }
      case 'ComponentAssembled': {
        const consumed = d.finalComponent?.components?.length ?? 0;
        return `Produzida a peça ${d.finalComponent?.name ?? '—'} consumindo ${consumed} material(is) refinado(s).`;
      }
      case 'ProductAssembled': {
        const consumed = d.finalProduct?.components?.length ?? 0;
        return `Montado ${d.finalProduct?.name ?? 'produto'} a partir de ${consumed} componente(s).`;
      }
      case 'InventoryUpdated':
        return `${d.componentName ?? 'Produto'}: stock passou de ${d.quantityBefore ?? 0} para ${d.quantityAfter ?? 0}.`;
      default:
        return `Evento ${event.eventType}.`;
    }
  }

  /** O 'purpose' do enunciado (para que serve o item), quando existe. */
  purposeOf(event: KafkaEvent): string | null {
    const d: any = event.details || {};
    const node = d.material || d.processedMaterial || d.finalComponent || d.finalProduct;
    const p = node?.purpose;
    if (!p || !p.targetProduct) return null;
    return `para ${p.targetComponent ?? '—'} de ${p.targetProduct}`;
  }

  /** Nó da árvore de dependências, se o evento transportar um. */
  treeNode(event: KafkaEvent): any | null {
    const d: any = event.details || {};
    const node = d.finalProduct || d.finalComponent || d.processedMaterial;
    return node?.components?.length ? node : null;
  }

  private qty(q: any, unit: any): string {
    if (q == null) return 'material';
    return `${q}${unit ? ' ' + unit : ''}`;
  }

  short(id: string): string {
    return id ? id.substring(0, 8) : '—';
  }

  isNew(event: KafkaEvent): boolean {
    return this.newEvents.has(event.eventId);
  }

  toggleRaw(id: string): void {
    this.showRaw[id] = !this.showRaw[id];
  }

  clear(): void {
    this.eventService.clearEvents();
  }
}
