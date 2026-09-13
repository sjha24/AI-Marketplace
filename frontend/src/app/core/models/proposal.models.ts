export type ProposalStatus =
  | 'SUBMITTED'
  | 'SHORTLISTED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface Proposal {
  id: number;
  jobId: number;
  jobTitle: string;
  freelancerId: number;
  freelancerDisplayName: string;
  clientId: number | null;
  coverLetter: string;
  bidAmount: number;
  currency: string;
  deliveryDays: number;
  status: ProposalStatus;
  aiGenerated: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SubmitProposalRequest {
  coverLetter: string;
  bidAmount: number;
  currency: string;
  deliveryDays: number;
}
