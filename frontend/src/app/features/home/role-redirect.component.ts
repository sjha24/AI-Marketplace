import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-role-redirect',
  standalone: true,
  template: '<p class="page">Opening your workspace…</p>'
})
export class RoleRedirectComponent implements OnInit {
  constructor(private readonly auth: AuthService) {}

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (user) {
      this.auth.navigateHome(user.role);
      return;
    }
    this.auth.me().subscribe({
      next: current => this.auth.navigateHome(current.role),
      error: () => this.auth.logout()
    });
  }
}
