import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-freelancer-home',
  standalone: true,
  imports: [RouterLink],
  template: `<section class="page"><p class="eyebrow">Freelancer workspace</p>
    <h1>Build your AI career</h1><p>Browse jobs, submit proposals, and track accepted orders.</p>
    <div style="display:flex;gap:.75rem;flex-wrap:wrap">
      <a class="btn primary" routerLink="/jobs">Browse jobs</a>
      <a class="btn" routerLink="/proposals/mine">My proposals</a>
      <a class="btn" routerLink="/orders">Orders</a>
      <a class="btn" routerLink="/profile">Complete your profile</a>
    </div></section>`
})
export class FreelancerHomeComponent {}
