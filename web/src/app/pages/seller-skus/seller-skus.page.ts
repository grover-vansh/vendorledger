import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Product } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';

const PRODUCT_TYPES = ['GOODS', 'SERVICE', 'PERISHABLE', 'OTHER'] as const;

@Component({
  selector: 'app-seller-skus',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './seller-skus.page.html'
})
export class SellerSkusPage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(PaydueApi);
  private readonly fb = inject(FormBuilder);

  readonly productTypes = PRODUCT_TYPES;
  readonly loading = signal(true);
  readonly error = signal('');
  readonly formError = signal('');
  readonly saving = signal(false);
  readonly products = signal<Product[]>([]);
  readonly query = signal('');

  readonly form = this.fb.nonNullable.group({
    skuCode: ['', Validators.required],
    name: ['', Validators.required],
    productType: ['OTHER'],
    shelfLifeDays: this.fb.control<number | null>(null)
  });

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const list = this.products();
    if (!q) {
      return list;
    }
    return list.filter((product) => {
      return (
        product.skuCode.toLowerCase().includes(q) ||
        product.name.toLowerCase().includes(q) ||
        product.productType.toLowerCase().includes(q)
      );
    });
  });

  constructor() {
    this.load();
  }

  setQuery(value: string): void {
    this.query.set(value);
  }

  createSku(): void {
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

    const { skuCode, name, productType, shelfLifeDays } = this.form.getRawValue();
    this.saving.set(true);
    this.api
      .createProduct(supplierId, {
        skuCode: skuCode.trim(),
        name: name.trim(),
        productType,
        shelfLifeDays: shelfLifeDays && shelfLifeDays > 0 ? shelfLifeDays : null
      })
      .subscribe({
        next: (created) => {
          this.products.update((list) => [...list, created]);
          this.form.reset({ skuCode: '', name: '', productType: 'OTHER', shelfLifeDays: null });
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

    this.api.listProducts(supplierId).subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
