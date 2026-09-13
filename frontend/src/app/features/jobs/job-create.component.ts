import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  applyServerErrors, clearServerErrorOnEdit, clearServerErrors, parseApiError
} from '../../core/http/api-error';
import { Category, CreateJobRequest, ExperienceLevel } from '../../core/models/job.models';
import { Skill } from '../../core/models/auth.models';
import { JobService } from '../../core/services/job.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'app-job-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './job-create.component.html',
  styleUrl: './job-create.component.scss'
})
export class JobCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly jobs = inject(JobService);
  private readonly profiles = inject(ProfileService);
  private readonly router = inject(Router);

  readonly form = this.fb.group({
    categoryId: this.fb.nonNullable.control('', Validators.required),
    title: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(10000)]),
    budgetMin: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    budgetMax: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    currency: this.fb.nonNullable.control('INR', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]),
    experienceLevel: this.fb.nonNullable.control<ExperienceLevel>('INTERMEDIATE', Validators.required),
    closesAt: this.fb.control<string | null>(null)
  });

  categories: Category[] = [];
  skills: Skill[] = [];
  selectedSkillIds = new Set<number>();
  loading = true;
  saving = false;
  error = '';
  skillError = '';

  constructor() {
    clearServerErrorOnEdit(this.form);
    forkJoin({
      categories: this.jobs.getCategories(),
      skills: this.profiles.getSkills()
    }).subscribe({
      next: ({ categories, skills }) => {
        this.categories = categories;
        this.skills = skills;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load form data.').message;
      }
    });
  }

  toggleSkill(skillId: number, checked: boolean): void {
    checked ? this.selectedSkillIds.add(skillId) : this.selectedSkillIds.delete(skillId);
  }

  isSelected(skillId: number): boolean {
    return this.selectedSkillIds.has(skillId);
  }

  submit(): void {
    this.error = '';
    this.skillError = '';
    clearServerErrors(this.form);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.selectedSkillIds.size) {
      this.skillError = 'Select at least one skill.';
      return;
    }

    this.saving = true;
    const raw = this.form.getRawValue();
    const request: CreateJobRequest = {
      categoryId: Number(raw.categoryId),
      title: raw.title.trim(),
      description: raw.description.trim(),
      budgetMin: Number(raw.budgetMin),
      budgetMax: Number(raw.budgetMax),
      currency: raw.currency.toUpperCase(),
      experienceLevel: raw.experienceLevel,
      closesAt: raw.closesAt ? new Date(raw.closesAt).toISOString() : null,
      skillIds: [...this.selectedSkillIds]
    };

    this.jobs.createJob(request).subscribe({
      next: job => {
        this.saving = false;
        void this.router.navigate(['/jobs', job.id]);
      },
      error: err => {
        this.saving = false;
        const api = parseApiError(err, 'Could not create job.');
        const unmatched = applyServerErrors(this.form, api.details);
        if (api.details?.['skillIds']) {
          this.skillError = api.details['skillIds'];
        }
        if (!api.details || unmatched.length || api.status === 0) {
          this.error = unmatched.join(' ') || api.message;
        }
      }
    });
  }
}
