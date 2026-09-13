import { DatePipe } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { parseApiError } from '../../core/http/api-error';
import { ChatMessage, ParticipantPresence } from '../../core/models/chat.models';
import { AuthService } from '../../core/services/auth.service';
import { ChatService } from '../../core/services/chat.service';
import { ChatSocketService } from '../../core/services/chat-socket.service';

@Component({
  selector: 'app-order-chat',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './order-chat.component.html',
  styleUrl: './order-chat.component.scss'
})
export class OrderChatComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) orderId!: number;
  @Input() peerLabel = 'Participant';

  private readonly chatApi = inject(ChatService);
  private readonly socket = inject(ChatSocketService);
  private readonly auth = inject(AuthService);

  messages: ChatMessage[] = [];
  participants: ParticipantPresence[] = [];
  draft = '';
  loading = true;
  sending = false;
  error = '';
  unreadCount = 0;
  private liveSub?: Subscription;
  private presenceTimer?: ReturnType<typeof setInterval>;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['orderId'] && this.orderId) {
      this.bootstrap();
    }
  }

  ngOnDestroy(): void {
    this.teardown();
  }

  get peerOnline(): boolean {
    const me = this.auth.currentUser()?.id;
    return this.participants.some(p => p.userId !== me && p.online);
  }

  get peerName(): string {
    const me = this.auth.currentUser()?.id;
    const peer = this.participants.find(p => p.userId !== me);
    return peer?.displayName ?? this.peerLabel;
  }

  send(): void {
    const body = this.draft.trim();
    if (!body || this.sending) {
      return;
    }
    this.sending = true;
    this.error = '';
    this.chatApi.sendMessage(this.orderId, body).subscribe({
      next: message => {
        this.appendMessage(message);
        this.draft = '';
        this.sending = false;
      },
      error: err => {
        this.sending = false;
        this.error = parseApiError(err, 'Could not send message.').message;
      }
    });
  }

  private bootstrap(): void {
    this.teardown();
    this.loading = true;
    this.error = '';
    this.messages = [];

    this.chatApi.getConversation(this.orderId).subscribe({
      next: conversation => {
        this.participants = conversation.participants;
        this.unreadCount = conversation.unreadCount;
      },
      error: err => {
        this.error = parseApiError(err, 'Chat is unavailable for this order.').message;
        this.loading = false;
      }
    });

    this.chatApi.getMessages(this.orderId, 0, 50).subscribe({
      next: page => {
        this.messages = page.content;
        this.loading = false;
        this.chatApi.markConversationRead(this.orderId).subscribe({
          next: () => { this.unreadCount = 0; }
        });
        void this.socket.connect(this.orderId);
        this.liveSub = this.socket.messages$.subscribe(message => {
          if (message.orderId !== this.orderId) {
            return;
          }
          this.appendMessage(message);
          if (!message.mine) {
            this.chatApi.markMessageRead(message.id).subscribe();
          }
        });
        this.presenceTimer = setInterval(() => this.refreshPresence(), 15000);
        this.refreshPresence();
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load messages.').message;
      }
    });
  }

  private refreshPresence(): void {
    this.chatApi.getPresence(this.orderId).subscribe({
      next: presence => {
        this.participants = presence.participants;
      }
    });
  }

  private appendMessage(message: ChatMessage): void {
    if (this.messages.some(m => m.id === message.id)) {
      return;
    }
    const me = this.auth.currentUser()?.id;
    const mine = message.senderId != null && message.senderId === me;
    this.messages = [...this.messages, { ...message, mine }];
  }

  private teardown(): void {
    this.liveSub?.unsubscribe();
    this.liveSub = undefined;
    if (this.presenceTimer) {
      clearInterval(this.presenceTimer);
      this.presenceTimer = undefined;
    }
    this.socket.disconnect();
  }
}
