import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const authed = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authed).pipe(
    catchError((err: HttpErrorResponse) => {
      const isAuthCall =
        req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');
      if (err.status === 401 && !isAuthCall) {
        auth.logout();
        void router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};
