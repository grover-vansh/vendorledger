import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Buyer } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';

function optionalGstin(control: AbstractControl): ValidationErrors | null {
  const raw = String(control.value ?? '').replace(/\s+/g, '');
  if (!raw) {
    return null;
  }
  return raw.length === 15 ? null : { gstin: true };
}

@Component({
  selector: 'app-seller-buyers',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './seller-buyers.page.html'
})
export class SellerBuyersPage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(PaydueApi);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly formError = signal('');
  readonly saving = signal(false);
  readonly buyers = signal<Buyer[]>([]);
  readonly query = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    gstin: ['', optionalGstin],
    phone: ['', Validators.maxLength(20)],
    contactName: ['', Validators.maxLength(200)],
    billingAddress: ['', Validators.maxLength(500)]
  });

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const list = this.buyers();
    if (!q) {
      return list;
    }
    return list.filter((buyer) => {
      return (
        buyer.name.toLowerCase().includes(q) ||
        (buyer.email ?? '').toLowerCase().includes(q) ||
        (buyer.gstin ?? '').toLowerCase().includes(q) ||
        (buyer.phone ?? '').toLowerCase().includes(q) ||
        (buyer.contactName ?? '').toLowerCase().includes(q)
      );
    });
  });

  constructor() {
    this.load();
  }

  setQuery(value: string): void {
    this.query.set(value);
  }

  createBuyer(): void {
    this.formError.set('');
    const supplierId = this.auth.user()?.supplierId;
    if (!supplierId) {
      this.formError.set('This seller account is not linked to a supplier company.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, email, gstin, phone, contactName, billingAddress } = this.form.getRawValue();
    this.saving.set(true);
    this.api
      .createBuyer(supplierId, {
        name: name.trim(),
        email: email.trim().toLowerCase(),
        gstin: gstin.replace(/\s+/g, '').trim().toUpperCase() || null,
        phone: phone.trim() || null,
        contactName: contactName.trim() || null,
        billingAddress: billingAddress.trim() || null
      })
      .subscribe({
        next: (created) => {
          this.buyers.update((list) => [...list, created].sort((a, b) => a.name.localeCompare(b.name)));
          this.form.reset({
            name: '',
            email: '',
            gstin: '',
            phone: '',
            contactName: '',
            billingAddress: ''
          });
          this.saving.set(false);
        },
        error: (err) => {
          this.formError.set(apiErrorMessage(err));
          this.saving.set(false);
        }
      });
  }

  private load(): void {
    const supplierId = this.auth.user()?.supplierId;
    if (!supplierId) {
      this.loading.set(false);
      this.error.set('This seller account is not linked to a supplier company.');
      return;
    }

    this.api.listMyBuyers(supplierId).subscribe({
      next: (buyers) => {
        this.buyers.set(buyers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
