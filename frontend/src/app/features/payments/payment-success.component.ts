import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.models';
import { parseApiError } from '../../core/http/api-error';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page">
      <header>
        <p class="eyebrow">Payment</p>
        <h1>Payment submitted</h1>
        <p>If the provider webhook has arrived, the order will show PAID_ESCROW.</p>
      </header>
      @if (error) { <p class="banner error" role="alert">{{ error }}</p> }
      @if (order) {
        <article class="panel">
          <p><strong>Order:</strong> {{ order.orderNumber }}</p>
          <p><strong>Status:</strong> {{ order.status }}</p>
          <p><strong>Amount:</strong> {{ order.currency }} {{ order.amount }}</p>
        </article>
        <div class="actions">
          <a class="btn primary" [routerLink]="['/orders', order.id]">View order</a>
          <a class="btn" routerLink="/orders">All orders</a>
        </div>
      }
    </section>
  `
})
export class PaymentSuccessComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly ordersApi = inject(OrderService);

  order: Order | null = null;
  error = '';

  constructor() {
    const orderId = Number(this.route.snapshot.queryParamMap.get('orderId'));
    if (!orderId) {
      this.error = 'Missing order id.';
      return;
    }
    this.ordersApi.getOrder(orderId).subscribe({
      next: order => { this.order = order; },
      error: err => { this.error = parseApiError(err, 'Could not refresh order.').message; }
    });
  }
}
