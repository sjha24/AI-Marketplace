import { Routes } from '@angular/router';
import {
  authGuard, clientGuard, freelancerGuard, guestGuard
} from './core/guards/auth.guards';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { ClientHomeComponent } from './features/home/client-home.component';
import { FreelancerHomeComponent } from './features/home/freelancer-home.component';
import { JobCreateComponent } from './features/jobs/job-create.component';
import { JobDetailComponent } from './features/jobs/job-detail.component';
import { JobFeedComponent } from './features/jobs/job-feed.component';
import { MyJobsComponent } from './features/jobs/my-jobs.component';
import { OrderDetailComponent } from './features/orders/order-detail.component';
import { OrdersListComponent } from './features/orders/orders-list.component';
import { PaymentFailedComponent } from './features/payments/payment-failed.component';
import { PaymentSuccessComponent } from './features/payments/payment-success.component';
import { MyProposalsComponent } from './features/proposals/my-proposals.component';
import { RoleRedirectComponent } from './features/home/role-redirect.component';
import { ProfileComponent } from './features/profile/profile.component';
import { ShellComponent } from './shared/layout/shell.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: RoleRedirectComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'jobs', component: JobFeedComponent },
      { path: 'jobs/new', component: JobCreateComponent, canActivate: [clientGuard] },
      { path: 'jobs/mine', component: MyJobsComponent, canActivate: [clientGuard] },
      { path: 'jobs/:id', component: JobDetailComponent },
      { path: 'proposals/mine', component: MyProposalsComponent, canActivate: [freelancerGuard] },
      { path: 'orders', component: OrdersListComponent },
      { path: 'orders/:id', component: OrderDetailComponent },
      { path: 'payments/success', component: PaymentSuccessComponent },
      { path: 'payments/failed', component: PaymentFailedComponent },
      { path: 'client', component: ClientHomeComponent, canActivate: [clientGuard] },
      { path: 'freelancer', component: FreelancerHomeComponent, canActivate: [freelancerGuard] }
    ]
  },
  { path: '**', redirectTo: '' }
];
