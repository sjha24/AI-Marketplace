import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Profile, Skill, UpdateProfileRequest, UpdateSkillsRequest
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  constructor(private readonly http: HttpClient) {}

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(`${environment.apiUrl}/profiles/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<Profile> {
    return this.http.put<Profile>(`${environment.apiUrl}/profiles/me`, request);
  }

  getSkills(): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${environment.apiUrl}/skills`);
  }

  updateSkills(request: UpdateSkillsRequest): Observable<Profile> {
    return this.http.put<Profile>(`${environment.apiUrl}/profiles/me/skills`, request);
  }
}
