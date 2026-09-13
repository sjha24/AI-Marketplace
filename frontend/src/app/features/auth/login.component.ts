import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  applyServerErrors, clearServerErrorOnEdit, clearServerErrors, parseApiError
} from '../../core/http/api-error';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  loading = false;
  error = '';

  constructor() { clearServerErrorOnEdit(this.form); }

  submit(): void {
    this.error = '';
    clearServerErrors(this.form);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.login(this.form.getRawValue()).subscribe({
      next: response => {
        this.loading = false;
        this.auth.navigateHome(response.role);
      },
      error: err => {
        this.loading = false;
        const api = parseApiError(err, 'Login failed.');
        const unmatched = applyServerErrors(this.form, api.details);
        if (!api.details || unmatched.length || api.status === 0 || api.status === 401) {
          this.error = unmatched.join(' ') || api.message;
        }
      }
    });
  }
}
