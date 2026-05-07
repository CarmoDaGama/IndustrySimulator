import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, interval } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { KafkaEvent } from '../models';
import { WebSocketService } from './websocket.service';

/**
 * EventService
 * Manages real-time events from Kafka and polling mechanisms
 * Simulates WebSocket-like behavior using RxJS observables
 */
@Injectable({
  providedIn: 'root',
})
export class EventService {
  // Observable for Kafka events
  private kafkaEvents$ = new BehaviorSubject<KafkaEvent[]>([]);
  kafkaEvents = this.kafkaEvents$.asObservable();

  // Observable for production status
  private productionStatus$ = new BehaviorSubject<any>({
    isRunning: false,
    currentStep: 0,
    totalSteps: 0,
    progress: 0,
    logs: [],
  });
  productionStatus = this.productionStatus$.asObservable();

  // Observable for inventory updates
  private inventoryUpdates$ = new BehaviorSubject<any>({
    lastUpdate: new Date(),
    items: [],
  });
  inventoryUpdates = this.inventoryUpdates$.asObservable();

  constructor(private websocketService: WebSocketService) {
    this.initializeEventSubscription();
  }

  /**
   * Initialize event subscription from WebSocket
   */
  private initializeEventSubscription(): void {
    this.websocketService.kafkaEvents$.subscribe((event) => {
      console.log('EventService: Received Kafka event from WebSocket:', event);
      this.addEvent(event);
    });
  }

  /**
   * Add a new event to the event stream
   */
  addEvent(event: KafkaEvent): void {
    const currentEvents = this.kafkaEvents$.value;
    const updatedEvents = [event, ...currentEvents].slice(0, 50); // Keep last 50 events
    this.kafkaEvents$.next(updatedEvents);
  }

  /**
   * Update production status
   */
  updateProductionStatus(status: any): void {
    this.productionStatus$.next(status);
  }

  /**
   * Update inventory
   */
  updateInventory(items: any[]): void {
    this.inventoryUpdates$.next({
      lastUpdate: new Date(),
      items,
    });
  }

  /**
   * Get recent events
   */
  getRecentEvents(limit: number = 10): KafkaEvent[] {
    return this.kafkaEvents$.value.slice(0, limit);
  }

  /**
   * Clear all events
   */
  clearEvents(): void {
    this.kafkaEvents$.next([]);
  }

  /**
   * Start production simulation
   */
  startProduction(steps: number, stepDuration: number): void {
    let currentStep = 0;
    const interval$ = interval(stepDuration);

    interval$.subscribe(() => {
      if (currentStep <= steps) {
        const progress = Math.round((currentStep / steps) * 100);
        this.productionStatus$.next({
          isRunning: currentStep < steps,
          currentStep,
          totalSteps: steps,
          progress,
          logs: [
            ...this.productionStatus$.value.logs,
            {
              timestamp: new Date(),
              message: `Etapa ${currentStep}/${steps} concluída`,
            },
          ],
        });
        currentStep++;
      }
    });
  }

  // ============================================================================
  // MOCK DATA GENERATORS (for testing without backend)
  // ============================================================================

  private mockEventIndex = 0;

  private generateMockKafkaEvents(): KafkaEvent[] {
    // This would be replaced with real WebSocket or polling data
    const eventTypes = [
      'RawMaterialProduced',
      'ProcessingCompleted',
      'ComponentAssembled',
      'ProductAssembled',
      'InventoryUpdated',
    ];

    const mockEvents: KafkaEvent[] = [];

    return this.kafkaEvents$.value; // Return current events when backend is ready
  }

  private generateMockProductionStatus(): any {
    return this.productionStatus$.value; // Return current status when backend is ready
  }
}
