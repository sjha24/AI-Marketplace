export type UserRole = 'CLIENT' | 'FREELANCER';
export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest extends LoginRequest {
  displayName: string;
  role: UserRole;
}
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
  displayName: string;
}
export interface CurrentUser {
  id: number;
  email: string;
  role: UserRole;
  status?: string;
  displayName: string;
}
export interface Skill { id: number; name: string; slug: string; }
export interface ProfileSkill {
  skillId: number;
  name: string;
  slug: string;
  level: SkillLevel;
}
export interface Profile {
  userId: number;
  displayName: string;
  headline: string | null;
  bio: string | null;
  hourlyRate: number | null;
  currency: string;
  location: string | null;
  avatarUrl: string | null;
  yearsExperience: number | null;
  skills: ProfileSkill[];
}
export interface UpdateProfileRequest {
  displayName: string;
  headline: string | null;
  bio: string | null;
  hourlyRate: number | null;
  currency: string;
  location: string | null;
  avatarUrl: string | null;
  yearsExperience: number | null;
}
export interface UpdateSkillsRequest {
  skills: Array<{ skillId: number; level: SkillLevel }>;
}
