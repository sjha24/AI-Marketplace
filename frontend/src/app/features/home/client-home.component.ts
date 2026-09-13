import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [RouterLink],
  template: `<section class="page"><p class="eyebrow">Client workspace</p>
    <h1>Find the right AI talent</h1><p>Post jobs, review proposals, and track hired orders.</p>
    <div style="display:flex;gap:.75rem;flex-wrap:wrap">
      <a class="btn primary" routerLink="/jobs/new">Post a job</a>
      <a class="btn" routerLink="/jobs/mine">My jobs</a>
      <a class="btn" routerLink="/orders">Orders</a>
      <a class="btn" routerLink="/profile">Complete your profile</a>
    </div></section>`
})
export class ClientHomeComponent {}
