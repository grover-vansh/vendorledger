import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Invoice, Product, Supplier } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';
import { InvoiceTableComponent } from '../../shared/invoice-table.component';
import { formatInr } from '../../shared/money';

@Component({
  selector: 'app-seller-home',
  imports: [InvoiceTableComponent, RouterLink],
  templateUrl: './seller-home.page.html'
})
export class SellerHomePage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(PaydueApi);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly supplier = signal<Supplier | null>(null);
  readonly products = signal<Product[]>([]);
  readonly customerCount = signal(0);
  readonly invoices = signal<Invoice[]>([]);
  readonly formatInr = formatInr;

  constructor() {
    this.load();
  }

  outstanding(): number {
    return this.invoices()
      .filter((inv) => inv.status !== 'PAID')
      .reduce((sum, inv) => sum + Number(inv.totalAmount), 0);
  }

  overdueCount(): number {
    return this.invoices().filter((inv) => inv.daysOverdue > 0).length;
  }

  private load(): void {
    const supplierId = this.auth.user()?.supplierId;
    if (!supplierId) {
      this.loading.set(false);
      this.error.set('This seller account is not linked to a supplier company.');
      return;
    }

    forkJoin({
      supplier: this.api.getSupplier(supplierId),
      products: this.api.listProducts(supplierId),
      buyers: this.api.listMyBuyers(supplierId),
      invoices: this.api.listSupplierInvoices(supplierId)
    }).subscribe({
      next: (data) => {
        this.supplier.set(data.supplier);
        this.products.set(data.products);
        this.customerCount.set(data.buyers.length);
        this.invoices.set(data.invoices);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
