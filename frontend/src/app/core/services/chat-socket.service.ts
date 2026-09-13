import { Injectable, OnDestroy } from '@angular/core';
import type { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ChatMessage } from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatSocketService implements OnDestroy {
  private client: Client | null = null;
  private orderSubscription: StompSubscription | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private readonly messageSubject = new Subject<ChatMessage>();
  readonly messages$ = this.messageSubject.asObservable();

  constructor(private readonly auth: AuthService) {}

  async connect(orderId: number): Promise<void> {
    this.disconnect();
    const token = this.auth.token;
    if (!token) {
      return;
    }

    // Lazy-load so login/home bundles never evaluate sockjs (needs `global`).
    const [{ Client }, SockJSModule] = await Promise.all([
      import('@stomp/stompjs'),
      import('sockjs-client')
    ]);
    const SockJS = SockJSModule.default;

    const wsBase = environment.apiUrl.replace(/\/api$/, '');
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsBase}/ws`) as WebSocket,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.orderSubscription = this.client!.subscribe(
          `/topic/orders.${orderId}.chat`,
          (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as ChatMessage;
              this.messageSubject.next(payload);
            } catch {
              // ignore malformed frames
            }
          }
        );
        this.startHeartbeat();
      }
    });
    this.client.activate();
  }

  send(orderId: number, body: string): void {
    if (!this.client?.connected) {
      return;
    }
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ orderId, body })
    });
  }

  disconnect(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
    this.orderSubscription?.unsubscribe();
    this.orderSubscription = null;
    if (this.client) {
      void this.client.deactivate();
      this.client = null;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.messageSubject.complete();
  }

  private startHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
    }
    this.heartbeatTimer = setInterval(() => {
      if (this.client?.connected) {
        this.client.publish({ destination: '/app/presence.heartbeat', body: '{}' });
      }
    }, 20000);
  }
}
