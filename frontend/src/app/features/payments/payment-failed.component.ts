import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-payment-failed',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page">
      <header>
        <p class="eyebrow">Payment</p>
        <h1>Payment failed or cancelled</h1>
        <p>{{ reason || 'No charge was completed. You can try again from the order page.' }}</p>
      </header>
      <div class="actions">
        @if (orderId) {
          <a class="btn primary" [routerLink]="['/orders', orderId]">Back to order</a>
        }
        <a class="btn" routerLink="/orders">All orders</a>
      </div>
    </section>
  `
})
export class PaymentFailedComponent {
  private readonly route = inject(ActivatedRoute);
  readonly orderId = Number(this.route.snapshot.queryParamMap.get('orderId')) || null;
  readonly reason = this.route.snapshot.queryParamMap.get('reason');
}
