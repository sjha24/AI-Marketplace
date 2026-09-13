import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { parseApiError } from '../../core/http/api-error';
import { Order } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <section class="page">
      <header>
        <p class="eyebrow">Orders</p>
        <h1>My orders</h1>
        <p>Orders created when a proposal is accepted. Payment comes in Week 4.</p>
      </header>

      @if (error) { <p class="banner error" role="alert">{{ error }}</p> }
      @if (loading) {
        <p>Loading orders…</p>
      } @else if (!orders.length) {
        <p class="meta">No orders yet.</p>
      } @else {
        <ul class="job-list">
          @for (order of orders; track order.id) {
            <li>
              <a class="job-card" [routerLink]="['/orders', order.id]">
                <h2>{{ order.orderNumber }} · {{ order.jobTitle }}</h2>
                <p class="meta">{{ order.status }} · {{ order.currency }} {{ order.amount }}</p>
                <p class="meta">{{ order.clientDisplayName }} ↔ {{ order.freelancerDisplayName }} · {{ order.createdAt | date:'medium' }}</p>
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `
})
export class OrdersListComponent {
  private readonly ordersApi = inject(OrderService);

  orders: Order[] = [];
  loading = true;
  error = '';

  constructor() {
    this.ordersApi.getMine().subscribe({
      next: page => {
        this.orders = page.content;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load orders.').message;
      }
    });
  }
}
