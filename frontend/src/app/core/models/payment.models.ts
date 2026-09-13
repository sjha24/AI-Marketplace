export type PaymentProvider = 'RAZORPAY' | 'STRIPE' | 'SIMULATED';
export type PaymentMethodHint =
  | 'CARD'
  | 'UPI'
  | 'GPAY'
  | 'PHONEPE'
  | 'PAYTM'
  | 'NETBANKING'
  | 'WALLET'
  | 'APPLE_PAY'
  | 'GOOGLE_PAY'
  | 'LINK'
  | 'ANY';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

export interface ProviderMethods {
  provider: PaymentProvider;
  enabled: boolean;
  methods: PaymentMethodHint[];
  currencies: string[];
  notes: string;
}

export interface PaymentSession {
  paymentId: number;
  orderId: number;
  orderNumber: string;
  provider: PaymentProvider;
  preferredMethod: PaymentMethodHint;
  status: PaymentStatus;
  amount: number;
  currency: string;
  providerOrderId: string | null;
  providerPaymentId: string | null;
  clientSecret: string | null;
  publishableKey: string | null;
  checkout: Record<string, unknown>;
}

export interface CreatePaymentSessionRequest {
  provider: PaymentProvider;
  preferredMethod?: PaymentMethodHint;
}
