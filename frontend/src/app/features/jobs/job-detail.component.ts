import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  applyServerErrors, clearServerErrorOnEdit, clearServerErrors, parseApiError
} from '../../core/http/api-error';
import { Category, CreateJobRequest, ExperienceLevel, Job } from '../../core/models/job.models';
import { Proposal } from '../../core/models/proposal.models';
import { Skill } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { JobService } from '../../core/services/job.service';
import { ProfileService } from '../../core/services/profile.service';
import { ProposalService } from '../../core/services/proposal.service';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './job-detail.component.html',
  styleUrl: './job-detail.component.scss'
})
export class JobDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly jobs = inject(JobService);
  private readonly profiles = inject(ProfileService);
  private readonly proposalsApi = inject(ProposalService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

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

  readonly proposalForm = this.fb.group({
    coverLetter: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(5000)]),
    bidAmount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0.01)]),
    currency: this.fb.nonNullable.control('INR', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]),
    deliveryDays: this.fb.nonNullable.control(7, [Validators.required, Validators.min(1), Validators.max(365)])
  });

  job: Job | null = null;
  categories: Category[] = [];
  skills: Skill[] = [];
  proposals: Proposal[] = [];
  myProposal: Proposal | null = null;
  selectedSkillIds = new Set<number>();
  loading = true;
  saving = false;
  cancelling = false;
  submittingProposal = false;
  acceptingId: number | null = null;
  editing = false;
  error = '';
  success = '';
  skillError = '';
  proposalError = '';
  proposalSuccess = '';

  constructor() {
    clearServerErrorOnEdit(this.form);
    clearServerErrorOnEdit(this.proposalForm);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      job: this.jobs.getJob(id),
      categories: this.jobs.getCategories(),
      skills: this.profiles.getSkills()
    }).subscribe({
      next: ({ job, categories, skills }) => {
        this.job = job;
        this.categories = categories;
        this.skills = skills;
        this.selectedSkillIds = new Set(job.skills.map(skill => skill.skillId));
        this.loadForm(job);
        this.proposalForm.patchValue({ currency: job.currency, bidAmount: job.budgetMin });
        this.loading = false;
        this.loadRoleData(job.id);
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load job.').message;
      }
    });
  }

  get isOwner(): boolean {
    const user = this.auth.currentUser();
    return !!this.job && !!user && this.job.clientId === user.id;
  }

  get isFreelancer(): boolean {
    return this.auth.currentUser()?.role === 'FREELANCER';
  }

  get canEdit(): boolean {
    return this.isOwner && this.job?.status === 'OPEN';
  }

  get canPropose(): boolean {
    return this.isFreelancer && this.job?.status === 'OPEN' && !this.myProposal && !this.isOwner;
  }

  toggleSkill(skillId: number, checked: boolean): void {
    checked ? this.selectedSkillIds.add(skillId) : this.selectedSkillIds.delete(skillId);
  }

  isSelected(skillId: number): boolean {
    return this.selectedSkillIds.has(skillId);
  }

  startEdit(): void {
    if (!this.canEdit || !this.job) return;
    this.editing = true;
    this.success = '';
    this.error = '';
    this.loadForm(this.job);
  }

  cancelEdit(): void {
    this.editing = false;
    this.error = '';
    this.skillError = '';
    clearServerErrors(this.form);
  }

  save(): void {
    if (!this.job) return;
    this.error = '';
    this.success = '';
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

    this.jobs.updateJob(this.job.id, request).subscribe({
      next: job => {
        this.job = job;
        this.saving = false;
        this.editing = false;
        this.success = 'Job updated.';
      },
      error: err => {
        this.saving = false;
        const api = parseApiError(err, 'Could not update job.');
        const unmatched = applyServerErrors(this.form, api.details);
        if (api.details?.['skillIds']) this.skillError = api.details['skillIds'];
        if (!api.details || unmatched.length || api.status === 0) {
          this.error = unmatched.join(' ') || api.message;
        }
      }
    });
  }

  cancelJob(): void {
    if (!this.job || !this.canEdit) return;
    this.cancelling = true;
    this.error = '';
    this.jobs.cancelJob(this.job.id).subscribe({
      next: job => {
        this.job = job;
        this.cancelling = false;
        this.editing = false;
        this.success = 'Job cancelled.';
      },
      error: err => {
        this.cancelling = false;
        this.error = parseApiError(err, 'Could not cancel job.').message;
      }
    });
  }

  submitProposal(): void {
    if (!this.job || !this.canPropose) return;
    this.proposalError = '';
    this.proposalSuccess = '';
    clearServerErrors(this.proposalForm);
    if (this.proposalForm.invalid) {
      this.proposalForm.markAllAsTouched();
      return;
    }

    this.submittingProposal = true;
    const raw = this.proposalForm.getRawValue();
    this.proposalsApi.submit(this.job.id, {
      coverLetter: raw.coverLetter.trim(),
      bidAmount: Number(raw.bidAmount),
      currency: raw.currency.toUpperCase(),
      deliveryDays: Number(raw.deliveryDays)
    }).subscribe({
      next: proposal => {
        this.myProposal = proposal;
        this.submittingProposal = false;
        this.proposalSuccess = 'Proposal submitted.';
      },
      error: err => {
        this.submittingProposal = false;
        const api = parseApiError(err, 'Could not submit proposal.');
        const unmatched = applyServerErrors(this.proposalForm, api.details);
        this.proposalError = unmatched.join(' ') || api.message;
      }
    });
  }

  acceptProposal(proposal: Proposal): void {
    if (!this.isOwner) return;
    this.acceptingId = proposal.id;
    this.error = '';
    this.proposalsApi.accept(proposal.id).subscribe({
      next: order => {
        this.acceptingId = null;
        void this.router.navigate(['/orders', order.id]);
      },
      error: err => {
        this.acceptingId = null;
        this.error = parseApiError(err, 'Could not accept proposal.').message;
        this.loadRoleData(this.job!.id);
      }
    });
  }

  formatBudget(job: Job): string {
    return `${job.currency} ${job.budgetMin} – ${job.budgetMax}`;
  }

  skillNames(job: Job): string {
    return job.skills.map(skill => skill.name).join(', ');
  }

  private loadRoleData(jobId: number): void {
    const run = (): void => {
      if (this.isOwner) {
        this.proposalsApi.listForJob(jobId).subscribe({
          next: proposals => { this.proposals = proposals; },
          error: () => { this.proposals = []; }
        });
        return;
      }
      if (this.isFreelancer) {
        this.proposalsApi.getMine(0, 100).pipe(
          catchError(() => of(null))
        ).subscribe(page => {
          this.myProposal = page?.content.find(p => p.jobId === jobId) ?? null;
        });
      }
    };

    if (this.auth.currentUser()) {
      run();
      return;
    }
    this.auth.me().subscribe({ next: () => run(), error: () => {} });
  }

  private loadForm(job: Job): void {
    this.form.patchValue({
      categoryId: String(job.categoryId),
      title: job.title,
      description: job.description,
      budgetMin: job.budgetMin,
      budgetMax: job.budgetMax,
      currency: job.currency,
      experienceLevel: job.experienceLevel,
      closesAt: job.closesAt ? this.toLocalInput(job.closesAt) : null
    });
  }

  private toLocalInput(value: string): string {
    const date = new Date(value);
    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
  }
}
