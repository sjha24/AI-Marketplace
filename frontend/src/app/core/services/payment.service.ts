import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order } from '../models/order.models';
import {
  CreatePaymentSessionRequest,
  PaymentSession,
  ProviderMethods
} from '../models/payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private readonly http: HttpClient) {}

  listMethods(): Observable<ProviderMethods[]> {
    return this.http.get<ProviderMethods[]>(`${environment.apiUrl}/payments/methods`);
  }

  createSession(orderId: number, request: CreatePaymentSessionRequest): Observable<PaymentSession> {
    return this.http.post<PaymentSession>(`${environment.apiUrl}/orders/${orderId}/pay`, request);
  }

  simulate(orderId: number, provider: string = 'SIMULATED'): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/orders/${orderId}/pay/simulate`, { provider });
  }
}
