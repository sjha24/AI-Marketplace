import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { parseApiError } from '../../core/http/api-error';
import { Proposal } from '../../core/models/proposal.models';
import { ProposalService } from '../../core/services/proposal.service';

@Component({
  selector: 'app-my-proposals',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <section class="page">
      <header>
        <p class="eyebrow">Freelancer</p>
        <h1>My proposals</h1>
        <p>Track bids you have submitted across open jobs.</p>
      </header>

      @if (error) { <p class="banner error" role="alert">{{ error }}</p> }
      @if (loading) {
        <p>Loading proposals…</p>
      } @else if (!proposals.length) {
        <p class="meta">No proposals yet. <a routerLink="/jobs">Browse jobs</a></p>
      } @else {
        <ul class="job-list">
          @for (proposal of proposals; track proposal.id) {
            <li>
              <a class="job-card" [routerLink]="['/jobs', proposal.jobId]">
                <h2>{{ proposal.jobTitle }}</h2>
                <p class="meta">{{ proposal.status }} · {{ proposal.currency }} {{ proposal.bidAmount }} · {{ proposal.deliveryDays }} days</p>
                <p class="meta">Submitted {{ proposal.createdAt | date:'medium' }}</p>
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `
})
export class MyProposalsComponent {
  private readonly proposalsApi = inject(ProposalService);

  proposals: Proposal[] = [];
  loading = true;
  error = '';

  constructor() {
    this.proposalsApi.getMine().subscribe({
      next: page => {
        this.proposals = page.content;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = parseApiError(err, 'Could not load proposals.').message;
      }
    });
  }
}
