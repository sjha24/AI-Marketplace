export type OrderStatus =
  | 'CREATED'
  | 'AWAITING_PAYMENT'
  | 'PAID_ESCROW'
  | 'IN_PROGRESS'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'DISPUTED'
  | 'REFUNDED'
  | 'CANCELLED';

export interface Order {
  id: number;
  orderNumber: string;
  jobId: number;
  jobTitle: string;
  proposalId: number;
  clientId: number;
  clientDisplayName: string;
  freelancerId: number;
  freelancerDisplayName: string;
  amount: number;
  platformFee: number;
  freelancerPayout: number;
  currency: string;
  status: OrderStatus;
  deliveryNote: string | null;
  deliveredAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
