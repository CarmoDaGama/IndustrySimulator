import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject, Observable } from 'rxjs';

export interface OrderStatusEvent {
  orderId: string;
  status: string;
  productType: string;
  quantity: number;
  customerName: string;
  updatedAt: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private ws: WebSocket | null = null;
  private orderStatusSubject = new Subject<OrderStatusEvent>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);

  public orderStatus$ = this.orderStatusSubject.asObservable();
  public isConnected$ = this.connectionStatusSubject.asObservable();

  constructor() {}

  connect(): void {
    if (this.ws || this.connectionStatusSubject.value) {
      return; // Already connected
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocol}//${window.location.host}/ws/orders`;

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.connectionStatusSubject.next(true);
      this.subscribeToOrderUpdates();
    };

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // Handle STOMP frames
        if (message.command === 'MESSAGE') {
          const body = JSON.parse(message.body);
          this.orderStatusSubject.next(body);
        } else if (message.type === 'OrderStatusEvent') {
          // Direct JSON message
          this.orderStatusSubject.next(message);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    this.ws.onerror = (event) => {
      console.error('WebSocket error:', event);
      this.connectionStatusSubject.next(false);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.connectionStatusSubject.next(false);
      this.ws = null;
      // Auto-reconnect after 5 seconds
      setTimeout(() => this.connect(), 5000);
    };
  }

  private subscribeToOrderUpdates(): void {
    if (!this.ws) return;

    const subscribeMessage = {
      id: 'sub-1',
      type: 'SUBSCRIBE',
      destination: '/topic/orders/all'
    };

    this.ws.send(JSON.stringify(subscribeMessage));
  }

  subscribeToOrder(orderId: string): void {
    if (!this.ws) {
      this.connect();
      setTimeout(() => this.subscribeToOrder(orderId), 500);
      return;
    }

    const subscribeMessage = {
      id: `sub-order-${orderId}`,
      type: 'SUBSCRIBE',
      destination: `/topic/orders/${orderId}`
    };

    this.ws.send(JSON.stringify(subscribeMessage));
  }

  unsubscribeFromOrder(orderId: string): void {
    if (!this.ws) return;

    const unsubscribeMessage = {
      id: `unsub-order-${orderId}`,
      type: 'UNSUBSCRIBE'
    };

    this.ws.send(JSON.stringify(unsubscribeMessage));
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.connectionStatusSubject.next(false);
    }
  }

  isConnected(): boolean {
    return this.connectionStatusSubject.value;
  }
}
