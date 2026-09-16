import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { UserRole } from '../../core/models';

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss'
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly error = signal('');
  readonly submitting = signal(false);
  readonly roles: { value: UserRole; title: string; copy: string }[] = [
    { value: 'SELLER', title: 'Seller', copy: 'Issue invoices and watch the 45-day clock.' },
    { value: 'BUYER', title: 'Buyer', copy: 'See bills your suppliers have raised on you.' },
    { value: 'ADMIN', title: 'Admin', copy: 'Oversee users and supplier companies.' }
  ];

  readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', [Validators.required]],
      role: this.fb.nonNullable.control<UserRole>('SELLER'),
      name: ['']
    },
    { validators: matchPasswords }
  );

  constructor() {
    this.form.controls.role.valueChanges.subscribe(() => {
      this.form.controls.name.setErrors(null);
    });
  }

  needsCompany(): boolean {
    const role = this.form.controls.role.value;
    return role === 'SELLER' || role === 'BUYER';
  }

  companyLabel(): string {
    return this.form.controls.role.value === 'BUYER' ? 'Buyer company name' : 'Seller company name';
  }

  submit(): void {
    this.error.set('');
    if (this.needsCompany() && !this.form.controls.name.value.trim()) {
      this.form.controls.name.setErrors({ required: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { email, password, role, name } = this.form.getRawValue();
    this.auth
      .register({
        email: email.trim().toLowerCase(),
        password,
        role,
        name: this.needsCompany() ? name.trim() : undefined
      })
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(this.auth.homePath());
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(apiErrorMessage(err));
        }
      });
  }
}
