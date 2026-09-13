import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { parseApiError } from '../../core/http/api-error';
import { Category, JobStatus, JobSummary } from '../../core/models/job.models';
import { Skill } from '../../core/models/auth.models';
import { JobService } from '../../core/services/job.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'app-job-feed',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './job-feed.component.html',
  styleUrl: './job-feed.component.scss'
})
export class JobFeedComponent {
  private readonly fb = inject(FormBuilder);
  private readonly jobs = inject(JobService);
  private readonly profiles = inject(ProfileService);

  readonly filters = this.fb.nonNullable.group({
    q: '',
    categoryId: '',
    skillId: '',
    status: 'OPEN' as JobStatus
  });

  categories: Category[] = [];
  skills: Skill[] = [];
  results: JobSummary[] = [];
  loading = true;
  searching = false;
  error = '';
  page = 0;
  totalPages = 0;

  constructor() {
    forkJoin({
      categories: this.jobs.getCategories(),
      skills: this.profiles.getSkills(),
      feed: this.jobs.listJobs({ status: 'OPEN' })
    }).subscribe({
      next: ({ categories, skills, feed }) => {
        this.categories = categories;
        this.skills = skills;
        this.applyFeed(feed);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load jobs.').message;
      }
    });
  }

  search(): void {
    this.searching = true;
    this.error = '';
    const raw = this.filters.getRawValue();
    this.jobs.listJobs({
      q: raw.q.trim() || undefined,
      categoryId: raw.categoryId ? Number(raw.categoryId) : undefined,
      skillId: raw.skillId ? Number(raw.skillId) : undefined,
      status: raw.status,
      page: this.page
    }).subscribe({
      next: feed => {
        this.searching = false;
        this.applyFeed(feed);
      },
      error: err => {
        this.searching = false;
        this.error = parseApiError(err, 'Could not search jobs.').message;
      }
    });
  }

  changePage(next: number): void {
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.search();
  }

  formatBudget(job: JobSummary): string {
    return `${job.currency} ${job.budgetMin} – ${job.budgetMax}`;
  }

  private applyFeed(feed: { content: JobSummary[]; totalPages: number; page: number }): void {
    this.results = feed.content;
    this.totalPages = feed.totalPages;
    this.page = feed.page;
  }
}
