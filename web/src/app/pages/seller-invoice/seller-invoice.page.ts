import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Buyer, Product } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';
import { formatInr, lineTotal } from '../../shared/money';

type InvoiceLineGroup = FormGroup<{
  rowKey: FormControl<number>;
  productId: FormControl<number | null>;
  quantity: FormControl<number | null>;
  unitPrice: FormControl<number | null>;
}>;

@Component({
  selector: 'app-seller-invoice',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './seller-invoice.page.html'
})
export class SellerInvoicePage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(PaydueApi);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly formError = signal('');
  readonly saving = signal(false);
  readonly products = signal<Product[]>([]);
  readonly buyers = signal<Buyer[]>([]);
  readonly formatInr = formatInr;
  private nextRowKey = 1;

  readonly form = this.fb.nonNullable.group({
    buyerId: this.fb.control<number | null>(null, Validators.required),
    invoiceNumber: [this.defaultInvoiceNumber(), Validators.required],
    invoiceDate: [this.today(), Validators.required],
    lines: this.fb.array<InvoiceLineGroup>([])
  });

  get lines(): FormArray<InvoiceLineGroup> {
    return this.form.controls.lines;
  }

  constructor() {
    this.load();
  }

  addRow(): void {
    this.lines.push(this.newLine());
  }

  removeRow(index: number): void {
    this.lines.removeAt(index);
  }

  rowTotal(index: number): number {
    const row = this.lines.at(index)?.getRawValue();
    return lineTotal(row?.quantity, row?.unitPrice);
  }

  invoiceTotal(): number {
    return this.lines.controls.reduce((sum, _, index) => sum + this.rowTotal(index), 0);
  }

  productLabel(product: Product): string {
    return `${product.skuCode} — ${product.name}`;
  }

  buyerLabel(buyer: Buyer): string {
    return buyer.gstin ? `${buyer.name} — ${buyer.gstin}` : buyer.name;
  }

  submit(): void {
    this.formError.set('');
    const supplierId = this.auth.user()?.supplierId;
    if (!supplierId) {
      this.formError.set('This seller account is not linked to a supplier company.');
      return;
    }
    if (this.lines.length === 0) {
      this.formError.set('Add at least one SKU row before raising the invoice.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('Choose a buyer, fill invoice details, and set SKU, quantity, and price on every row.');
      return;
    }

    const value = this.form.getRawValue();
    const lines = value.lines.map((line) => ({
      productId: Number(line.productId),
      quantity: Number(line.quantity),
      unitPrice: Number(line.unitPrice)
    }));

    if (lines.some((line) => !Number.isFinite(line.productId) || line.quantity <= 0 || line.unitPrice < 0)) {
      this.formError.set('Each row needs a SKU, a quantity greater than 0, and a price of 0 or more.');
      return;
    }

    this.saving.set(true);
    this.api
      .createInvoice({
        supplierId,
        buyerId: Number(value.buyerId),
        invoiceNumber: value.invoiceNumber.trim(),
        invoiceDate: value.invoiceDate,
        lines
      })
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/seller');
        },
        error: (err) => {
          this.formError.set(apiErrorMessage(err));
          this.saving.set(false);
        }
      });
  }

  private newLine(): InvoiceLineGroup {
    return this.fb.group({
      rowKey: this.fb.nonNullable.control(this.nextRowKey++),
      productId: this.fb.control<number | null>(null, Validators.required),
      quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
      unitPrice: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)])
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private defaultInvoiceNumber(): string {
    const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 12);
    return `INV-${stamp}`;
  }

  private load(): void {
    const supplierId = this.auth.user()?.supplierId;
    if (!supplierId) {
      this.loading.set(false);
      this.error.set('This seller account is not linked to a supplier company.');
      return;
    }

    forkJoin({
      products: this.api.listProducts(supplierId),
      buyers: this.api.listMyBuyers(supplierId)
    }).subscribe({
      next: (data) => {
        this.products.set(data.products.filter((product) => product.active));
        this.buyers.set(data.buyers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
