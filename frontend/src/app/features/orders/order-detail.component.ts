import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { parseApiError } from '../../core/http/api-error';
import { Order, OrderStatus } from '../../core/models/order.models';
import { PaymentMethodHint, PaymentProvider, ProviderMethods } from '../../core/models/payment.models';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { openRazorpayCheckout } from '../../core/payments/checkout.helpers';
import { OrderChatComponent } from './order-chat.component';

const TIMELINE: OrderStatus[] = [
  'AWAITING_PAYMENT',
  'PAID_ESCROW',
  'IN_PROGRESS',
  'DELIVERED',
  'COMPLETED'
];

const CHAT_STATUSES: OrderStatus[] = [
  'PAID_ESCROW',
  'IN_PROGRESS',
  'DELIVERED',
  'COMPLETED',
  'DISPUTED'
];

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule, OrderChatComponent],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss'
})
export class OrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ordersApi = inject(OrderService);
  private readonly paymentsApi = inject(PaymentService);
  private readonly auth = inject(AuthService);

  order: Order | null = null;
  providers: ProviderMethods[] = [];
  loading = true;
  paying = false;
  error = '';
  success = '';
  selectedProvider: PaymentProvider = 'SIMULATED';
  selectedMethod: PaymentMethodHint = 'ANY';

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      order: this.ordersApi.getOrder(id),
      methods: this.paymentsApi.listMethods()
    }).subscribe({
      next: ({ order, methods }) => {
        this.order = order;
        this.providers = methods;
        this.pickDefaults(order.currency);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load order.').message;
      }
    });
  }

  get isClient(): boolean {
    const user = this.auth.currentUser();
    return !!this.order && !!user && this.order.clientId === user.id;
  }

  get canPay(): boolean {
    return this.isClient && this.order?.status === 'AWAITING_PAYMENT';
  }

  get canChat(): boolean {
    return !!this.order && CHAT_STATUSES.includes(this.order.status);
  }

  get chatPeerLabel(): string {
    if (!this.order) return 'Participant';
    return this.isClient ? this.order.freelancerDisplayName : this.order.clientDisplayName;
  }

  get enabledProviders(): ProviderMethods[] {
    return this.providers.filter(p => p.enabled);
  }

  get methodsForProvider(): PaymentMethodHint[] {
    const provider = this.enabledProviders.find(p => p.provider === this.selectedProvider);
    return provider?.methods ?? ['ANY'];
  }

  timelineSteps(): { status: OrderStatus; done: boolean; current: boolean }[] {
    if (!this.order) return [];
    const currentIndex = TIMELINE.indexOf(this.order.status);
    return TIMELINE.map((status, index) => ({
      status,
      done: currentIndex > index || this.order!.status === 'COMPLETED',
      current: this.order!.status === status
    }));
  }

  onProviderChange(): void {
    const methods = this.methodsForProvider;
    this.selectedMethod = methods.includes('ANY') ? 'ANY' : methods[0];
  }

  async startPay(): Promise<void> {
    if (!this.order || !this.canPay) return;
    this.paying = true;
    this.error = '';
    this.success = '';

    this.paymentsApi.createSession(this.order.id, {
      provider: this.selectedProvider,
      preferredMethod: this.selectedMethod
    }).subscribe({
      next: async session => {
        try {
          if (session.provider === 'SIMULATED') {
            this.paymentsApi.simulate(this.order!.id, 'SIMULATED').subscribe({
              next: order => {
                this.order = order;
                this.paying = false;
                this.success = 'Simulated payment captured — funds held in escrow.';
                void this.router.navigate(['/payments/success'], {
                  queryParams: { orderId: order.id }
                });
              },
              error: err => this.failPay(err)
            });
            return;
          }

          if (session.provider === 'RAZORPAY') {
            await openRazorpayCheckout({
              checkout: session.checkout,
              orderNumber: session.orderNumber,
              onSuccess: () => {
                this.paying = false;
                this.success = 'Payment submitted. Waiting for Razorpay webhook to mark escrow…';
                this.refreshSoon();
                void this.router.navigate(['/payments/success'], {
                  queryParams: { orderId: this.order!.id }
                });
              },
              onFailure: message => {
                this.paying = false;
                this.error = message;
                void this.router.navigate(['/payments/failed'], {
                  queryParams: { orderId: this.order!.id, reason: message }
                });
              }
            });
            return;
          }

          if (session.provider === 'STRIPE') {
            const checkoutUrl = String(session.checkout['checkoutUrl'] ?? '');
            if (!checkoutUrl) {
              this.paying = false;
              this.error = 'Stripe checkout URL missing.';
              return;
            }
            window.location.href = checkoutUrl;
            return;
          }

          this.paying = false;
          this.error = 'Unsupported provider response.';
        } catch (err) {
          this.failPay(err);
        }
      },
      error: err => this.failPay(err)
    });
  }

  private failPay(err: unknown): void {
    this.paying = false;
    this.error = parseApiError(err, 'Could not start payment.').message;
  }

  private refreshSoon(): void {
    setTimeout(() => {
      if (!this.order) return;
      this.ordersApi.getOrder(this.order.id).subscribe({
        next: order => { this.order = order; }
      });
    }, 2500);
  }

  private pickDefaults(currency: string): void {
    const enabled = this.enabledProviders;
    const preferred = currency.toUpperCase() === 'INR'
      ? enabled.find(p => p.provider === 'RAZORPAY') ?? enabled[0]
      : enabled.find(p => p.provider === 'STRIPE') ?? enabled[0];
    if (preferred) {
      this.selectedProvider = preferred.provider;
      this.onProviderChange();
    }
  }
}
