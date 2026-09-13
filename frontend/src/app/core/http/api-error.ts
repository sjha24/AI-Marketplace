import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';

export interface ApiError {
  message: string;
  details?: Record<string, string>;
  status: number;
}

export function parseApiError(error: unknown, fallback = 'Something went wrong.'): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { message: fallback, status: 0 };
  }
  const body = error.error;
  const details = body && typeof body === 'object' && isStringMap(body.details)
    ? body.details as Record<string, string>
    : undefined;
  const message = error.status === 0
    ? 'Unable to reach the server. Check your connection and try again.'
    : body && typeof body === 'object' && typeof body.message === 'string'
      ? body.message
      : fallback;
  return { message, details, status: error.status };
}

export function applyServerErrors(
  form: FormGroup,
  details: Record<string, string> | undefined
): string[] {
  const unmatched: string[] = [];
  for (const [field, message] of Object.entries(details ?? {})) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
    } else {
      unmatched.push(message);
    }
  }
  return unmatched;
}

export function clearServerErrors(control: AbstractControl): void {
  if (control instanceof FormGroup) {
    Object.values(control.controls).forEach(clearServerErrors);
  }
  if (!control.errors?.['server']) return;
  const { server: _server, ...remaining } = control.errors;
  control.setErrors(Object.keys(remaining).length ? remaining : null);
}

export function clearServerErrorOnEdit(form: FormGroup): void {
  Object.values(form.controls).forEach(control => {
    control.valueChanges.subscribe(() => clearServerErrors(control));
  });
}

function isStringMap(value: unknown): value is Record<string, string> {
  return !!value && typeof value === 'object'
    && Object.values(value as Record<string, unknown>).every(item => typeof item === 'string');
}
