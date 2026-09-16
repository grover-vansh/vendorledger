import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { roleLabel, Supplier, UserSummary } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';

@Component({
  selector: 'app-admin-home',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-home.page.html'
})
export class AdminHomePage {
  private readonly api = inject(PaydueApi);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly formError = signal('');
  readonly users = signal<UserSummary[]>([]);
  readonly suppliers = signal<Supplier[]>([]);
  readonly saving = signal(false);
  readonly roleLabel = roleLabel;
  readonly adminEmail = this.auth.user()?.email ?? '';

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: [''],
    gstin: ['']
  });

  constructor() {
    this.load();
  }

  sellerCount(): number {
    return this.users().filter((u) => u.role === 'SELLER').length;
  }

  buyerCount(): number {
    return this.users().filter((u) => u.role === 'BUYER').length;
  }

  createSupplier(): void {
    this.formError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const { name, email, gstin } = this.form.getRawValue();
    this.api
      .createSupplier({
        name: name.trim(),
        email: email.trim() || undefined,
        gstin: gstin.trim() || undefined,
        msme: true
      })
      .subscribe({
        next: (created) => {
          this.suppliers.update((list) => [...list, created]);
          this.form.reset({ name: '', email: '', gstin: '' });
          this.saving.set(false);
        },
        error: (err) => {
          this.formError.set(apiErrorMessage(err));
          this.saving.set(false);
        }
      });
  }

  private load(): void {
    forkJoin({
      users: this.api.listUsers(),
      suppliers: this.api.listSuppliers()
    }).subscribe({
      next: (data) => {
        this.users.set(data.users);
        this.suppliers.set(data.suppliers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
