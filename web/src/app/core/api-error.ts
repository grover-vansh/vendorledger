import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as { error?: string } | string | null;
    if (body && typeof body === 'object' && typeof body.error === 'string' && body.error) {
      return body.error;
    }
    if (err.status === 0) {
      return 'Cannot reach the PayDue API. Start Postgres and the Spring Boot app on port 8080.';
    }
    if (err.status === 401) {
      return 'Invalid email or password';
    }
    return err.statusText || 'Request failed';
  }
  return 'Something went wrong';
}
