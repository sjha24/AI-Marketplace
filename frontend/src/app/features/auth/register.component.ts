import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  applyServerErrors, clearServerErrorOnEdit, clearServerErrors, parseApiError
} from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    role: [null as UserRole | null, Validators.required]
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
    const value = this.form.getRawValue();
    this.auth.register({ ...value, role: value.role as UserRole }).subscribe({
      next: response => {
        this.loading = false;
        this.auth.navigateHome(response.role);
      },
      error: err => {
        this.loading = false;
        const api = parseApiError(err, 'Registration failed.');
        const unmatched = applyServerErrors(this.form, api.details);
        if (!api.details || unmatched.length || api.status === 0) {
          this.error = unmatched.join(' ') || api.message;
        }
      }
    });
  }
}
