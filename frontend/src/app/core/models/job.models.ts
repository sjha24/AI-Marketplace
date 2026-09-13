export type JobStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED';
export type ExperienceLevel = 'ENTRY' | 'INTERMEDIATE' | 'EXPERT';

export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
}

export interface JobSkill {
  skillId: number;
  name: string;
  slug: string;
}

export interface JobSummary {
  id: number;
  clientId: number;
  categoryId: number;
  categoryName: string;
  title: string;
  budgetMin: number;
  budgetMax: number;
  currency: string;
  experienceLevel: ExperienceLevel;
  status: JobStatus;
  createdAt: string;
  skillNames: string[];
}

export interface Job extends JobSummary {
  description: string;
  closesAt: string | null;
  updatedAt: string;
  skills: JobSkill[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateJobRequest {
  categoryId: number;
  title: string;
  description: string;
  budgetMin: number;
  budgetMax: number;
  currency: string;
  experienceLevel: ExperienceLevel;
  closesAt: string | null;
  skillIds: number[];
}

export interface JobListParams {
  q?: string;
  skillId?: number | null;
  categoryId?: number | null;
  status?: JobStatus;
  page?: number;
  size?: number;
}
