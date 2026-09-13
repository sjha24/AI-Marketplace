import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse, CurrentUser, LoginRequest, RegisterRequest, UserRole
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'ai-marketplace-token';
  private readonly tokenState = signal<string | null>(localStorage.getItem(this.storageKey));
  private readonly userState = signal<CurrentUser | null>(null);
  readonly currentUser = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenState());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  get token(): string | null {
    return this.tokenState();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap(response => this.acceptAuth(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap(response => this.acceptAuth(response)));
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${environment.apiUrl}/auth/me`)
      .pipe(tap(user => this.userState.set(user)));
  }

  logout(navigate = true): void {
    localStorage.removeItem(this.storageKey);
    this.tokenState.set(null);
    this.userState.set(null);
    if (navigate) void this.router.navigateByUrl('/login');
  }

  navigateHome(role = this.currentUser()?.role): void {
    void this.router.navigateByUrl(this.homeFor(role));
  }

  homeFor(role?: UserRole): string {
    return role === 'CLIENT' ? '/client' : role === 'FREELANCER' ? '/freelancer' : '/';
  }

  private acceptAuth(response: AuthResponse): void {
    localStorage.setItem(this.storageKey, response.accessToken);
    this.tokenState.set(response.accessToken);
    this.userState.set({
      id: response.userId,
      email: response.email,
      role: response.role,
      displayName: response.displayName
    });
  }
}
