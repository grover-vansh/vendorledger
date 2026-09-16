import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthResponse, RegisterRequest, UserRole, homePath } from './models';

const TOKEN_KEY = 'paydue.token';
const USER_KEY = 'paydue.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<AuthResponse | null>(readStoredUser());

  readonly user = computed(() => this.session());
  readonly loggedIn = computed(() => !!this.session()?.token);

  constructor(private readonly http: HttpClient) {}

  token(): string | null {
    return this.session()?.token ?? localStorage.getItem(TOKEN_KEY);
  }

  homePath(): string {
    const role = this.session()?.role;
    return role ? homePath(role) : '/login';
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((res) => this.persist(res)));
  }

  register(body: RegisterRequest) {
    return this.http
      .post<AuthResponse>('/api/auth/register', body)
      .pipe(tap((res) => this.persist(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.session.set(null);
  }

  hasRole(role: UserRole): boolean {
    return this.session()?.role === role;
  }

  private persist(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res));
    this.session.set(res);
  }
}

function readStoredUser(): AuthResponse | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const raw = localStorage.getItem(USER_KEY);
  if (!token || !raw) {
    return null;
  }
  try {
    const user = JSON.parse(raw) as AuthResponse;
    return { ...user, token };
  } catch {
    return null;
  }
}
