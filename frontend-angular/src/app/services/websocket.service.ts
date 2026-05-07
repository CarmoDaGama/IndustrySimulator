import { Injectable, OnDestroy } from '@angular/core';
import { Subject, BehaviorSubject, Observable } from 'rxjs';
import { Client, Message } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';

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
export class WebSocketService implements OnDestroy {
  private stompClient: Client | null = null;
  private orderStatusSubject = new Subject<OrderStatusEvent>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);

  public orderStatus$ = this.orderStatusSubject.asObservable();
  public isConnected$ = this.connectionStatusSubject.asObservable();

  // The Assembly Service is where the WebSocket endpoint is hosted
  private readonly WS_URL = 'http://localhost:8084/ws/orders';

  constructor() {}

  ngOnDestroy(): void {
    this.disconnect();
  }

  connect(): void {
    if (this.stompClient && this.stompClient.active) {
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.WS_URL),
      debug: (msg: string) => console.log('STOMP: ' + msg),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('STOMP Connected: ' + frame);
      this.connectionStatusSubject.next(true);
      
      // Subscribe to all orders by default
      this.subscribeToOrderUpdates();
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP Error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
      this.connectionStatusSubject.next(false);
    };

    this.stompClient.onWebSocketClose = () => {
      console.log('STOMP Connection Closed');
      this.connectionStatusSubject.next(false);
    };

    this.stompClient.activate();
  }

  private subscribeToOrderUpdates(): void {
    if (!this.stompClient || !this.stompClient.active) return;

    this.stompClient.subscribe('/topic/orders/all', (message: Message) => {
      if (message.body) {
        try {
          const event: OrderStatusEvent = JSON.parse(message.body);
          this.orderStatusSubject.next(event);
        } catch (e) {
          console.error('Error parsing order status message:', e);
        }
      }
    });
  }

  subscribeToOrder(orderId: string): void {
    if (!this.stompClient || !this.stompClient.active) {
      this.connect();
      // The onConnect handler will re-subscribe if needed, 
      // but for specific orders we might need to wait
      setTimeout(() => this.subscribeToOrder(orderId), 1000);
      return;
    }

    this.stompClient.subscribe(`/topic/orders/${orderId}`, (message: Message) => {
      if (message.body) {
        try {
          const event: OrderStatusEvent = JSON.parse(message.body);
          this.orderStatusSubject.next(event);
        } catch (e) {
          console.error(`Error parsing order ${orderId} message:`, e);
        }
      }
    });
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connectionStatusSubject.next(false);
    }
  }

  isConnected(): boolean {
    return this.connectionStatusSubject.value;
  }
}
