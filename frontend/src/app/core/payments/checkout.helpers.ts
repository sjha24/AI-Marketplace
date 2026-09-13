declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
    Stripe?: (key: string) => {
      elements: (opts?: Record<string, unknown>) => {
        create: (type: string, opts?: Record<string, unknown>) => {
          mount: (el: string | HTMLElement) => void;
        };
        getElement?: (type: string) => unknown;
      };
      confirmPayment: (opts: Record<string, unknown>) => Promise<{ error?: { message?: string } }>;
    };
  }
}

export function loadScript(src: string, id: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(id)) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.id = id;
    script.src = src;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error(`Failed to load ${src}`));
    document.body.appendChild(script);
  });
}

export async function openRazorpayCheckout(session: {
  checkout: Record<string, unknown>;
  orderNumber: string;
  onSuccess: () => void;
  onFailure: (message: string) => void;
}): Promise<void> {
  await loadScript('https://checkout.razorpay.com/v1/checkout.js', 'razorpay-checkout');
  if (!window.Razorpay) {
    throw new Error('Razorpay SDK failed to load');
  }
  const checkout = session.checkout;
  const options: Record<string, unknown> = {
    key: checkout['keyId'],
    amount: checkout['amount'],
    currency: checkout['currency'],
    name: checkout['name'] ?? 'AI Marketplace',
    description: checkout['description'] ?? session.orderNumber,
    order_id: checkout['orderId'] ?? checkout['providerOrderId'],
    prefill: checkout['prefill'] ?? {},
    theme: checkout['theme'] ?? { color: '#335cff' },
    handler: () => session.onSuccess(),
    modal: {
      ondismiss: () => session.onFailure('Payment cancelled')
    }
  };
  if (checkout['method']) {
    options['config'] = { display: { blocks: {}, hide: [], sequence: ['block.upi', 'block.card', 'block.wallet', 'block.netbanking'], preferences: { show_default_blocks: true } } };
    options['method'] = checkout['method'];
  }
  const rzp = new window.Razorpay(options);
  rzp.open();
}

export async function confirmStripePayment(session: {
  clientSecret: string;
  publishableKey: string;
  returnUrl: string;
}): Promise<{ error?: string }> {
  await loadScript('https://js.stripe.com/v3/', 'stripe-js');
  if (!window.Stripe) {
    return { error: 'Stripe SDK failed to load' };
  }
  const stripe = window.Stripe(session.publishableKey);
  const result = await stripe.confirmPayment({
    clientSecret: session.clientSecret,
    confirmParams: {
      return_url: session.returnUrl
    },
    redirect: 'if_required'
  });
  if (result.error) {
    return { error: result.error.message || 'Stripe payment failed' };
  }
  return {};
}
