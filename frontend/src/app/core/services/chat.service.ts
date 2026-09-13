import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/job.models';
import { ChatMessage, Conversation, PresenceResponse } from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  constructor(private readonly http: HttpClient) {}

  getConversation(orderId: number): Observable<Conversation> {
    return this.http.get<Conversation>(`${environment.apiUrl}/chat/orders/${orderId}`);
  }

  getMessages(orderId: number, page = 0, size = 50): Observable<PageResponse<ChatMessage>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ChatMessage>>(
      `${environment.apiUrl}/chat/orders/${orderId}/messages`,
      { params }
    );
  }

  sendMessage(orderId: number, body: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${environment.apiUrl}/chat/orders/${orderId}/messages`, { body });
  }

  markMessageRead(messageId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/chat/messages/${messageId}/read`, {});
  }

  markConversationRead(orderId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/chat/orders/${orderId}/read`, {});
  }

  getPresence(orderId: number): Observable<PresenceResponse> {
    return this.http.get<PresenceResponse>(`${environment.apiUrl}/chat/orders/${orderId}/presence`);
  }
}
