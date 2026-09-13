import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, switchMap } from 'rxjs';
import {
  applyServerErrors, clearServerErrorOnEdit, clearServerErrors, parseApiError
} from '../../core/http/api-error';
import {
  Profile, Skill, SkillLevel, UpdateProfileRequest
} from '../../core/models/auth.models';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly profiles = inject(ProfileService);
  readonly form = this.fb.group({
    displayName: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    headline: this.fb.nonNullable.control('', Validators.maxLength(200)),
    bio: this.fb.nonNullable.control(''),
    hourlyRate: this.fb.control<number | null>(null, Validators.min(0)),
    currency: this.fb.nonNullable.control('USD', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]),
    location: this.fb.nonNullable.control('', Validators.maxLength(120)),
    avatarUrl: this.fb.nonNullable.control('', Validators.maxLength(500)),
    yearsExperience: this.fb.control<number | null>(null, Validators.min(0))
  });
  skills: Skill[] = [];
  selected = new Map<number, SkillLevel>();
  loading = true;
  saving = false;
  error = '';
  skillError = '';
  success = '';

  constructor() {
    clearServerErrorOnEdit(this.form);
    forkJoin({ profile: this.profiles.getProfile(), skills: this.profiles.getSkills() }).subscribe({
      next: ({ profile, skills }) => {
        this.skills = skills;
        this.loadProfile(profile);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        const api = parseApiError(err, 'Could not load profile.');
        this.error = api.message;
        // Retry skills independently so a profile remains editable when the catalogue fails.
        this.profiles.getProfile().subscribe({ next: profile => this.loadProfile(profile) });
        this.profiles.getSkills().subscribe({
          next: skills => this.skills = skills,
          error: skillErr => this.skillError = parseApiError(skillErr, 'Could not load skills.').message
        });
      }
    });
  }

  isSelected(id: number): boolean { return this.selected.has(id); }

  toggleSkill(skill: Skill, checked: boolean): void {
    checked ? this.selected.set(skill.id, 'INTERMEDIATE') : this.selected.delete(skill.id);
  }

  setLevel(skillId: number, value: string): void {
    this.selected.set(skillId, value as SkillLevel);
  }

  submit(): void {
    this.error = '';
    this.success = '';
    clearServerErrors(this.form);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const raw = this.form.getRawValue();
    const request: UpdateProfileRequest = {
      displayName: raw.displayName.trim(),
      headline: this.blank(raw.headline),
      bio: this.blank(raw.bio),
      hourlyRate: raw.hourlyRate,
      currency: raw.currency.toUpperCase(),
      location: this.blank(raw.location),
      avatarUrl: this.blank(raw.avatarUrl),
      yearsExperience: raw.yearsExperience
    };
    this.profiles.updateProfile(request).pipe(
      switchMap(() => this.profiles.updateSkills({
        skills: [...this.selected].map(([skillId, level]) => ({ skillId, level }))
      }))
    ).subscribe({
      next: profile => {
        this.saving = false;
        this.loadProfile(profile);
        this.success = 'Profile saved.';
      },
      error: err => {
        this.saving = false;
        const api = parseApiError(err, 'Could not save profile.');
        const unmatched = applyServerErrors(this.form, api.details);
        if (!api.details || unmatched.length || api.status === 0) {
          this.error = unmatched.join(' ') || api.message;
        }
      }
    });
  }

  private loadProfile(profile: Profile): void {
    this.form.patchValue({
      displayName: profile.displayName,
      headline: profile.headline ?? '',
      bio: profile.bio ?? '',
      hourlyRate: profile.hourlyRate,
      currency: profile.currency ?? 'USD',
      location: profile.location ?? '',
      avatarUrl: profile.avatarUrl ?? '',
      yearsExperience: profile.yearsExperience
    });
    this.selected = new Map(profile.skills.map(skill => [skill.skillId, skill.level]));
  }

  private blank(value: string): string | null {
    return value.trim() || null;
  }
}
