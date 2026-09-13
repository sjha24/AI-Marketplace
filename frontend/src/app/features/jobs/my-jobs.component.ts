import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { parseApiError } from '../../core/http/api-error';
import { JobSummary } from '../../core/models/job.models';
import { JobService } from '../../core/services/job.service';

@Component({
  selector: 'app-my-jobs',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-jobs.component.html',
  styleUrl: './my-jobs.component.scss'
})
export class MyJobsComponent {
  private readonly jobs = inject(JobService);

  results: JobSummary[] = [];
  loading = true;
  error = '';
  page = 0;
  totalPages = 0;

  constructor() {
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.jobs.getMyJobs(page).subscribe({
      next: feed => {
        this.results = feed.content;
        this.page = feed.page;
        this.totalPages = feed.totalPages;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load your jobs.').message;
      }
    });
  }

  changePage(next: number): void {
    if (next < 0 || next >= this.totalPages) return;
    this.load(next);
  }

  formatBudget(job: JobSummary): string {
    return `${job.currency} ${job.budgetMin} – ${job.budgetMax}`;
  }
}
