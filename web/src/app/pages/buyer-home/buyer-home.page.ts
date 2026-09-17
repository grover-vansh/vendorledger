import { Component, inject, signal } from '@angular/core';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Invoice } from '../../core/models';
import { PaydueApi } from '../../core/paydue.api';
import { InvoiceTableComponent } from '../../shared/invoice-table.component';
import { formatInr } from '../../shared/money';

@Component({
  selector: 'app-buyer-home',
  imports: [InvoiceTableComponent],
  templateUrl: './buyer-home.page.html'
})
export class BuyerHomePage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(PaydueApi);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly companyName = this.auth.user()?.companyName || 'Your company';
  readonly invoices = signal<Invoice[]>([]);
  readonly formatInr = formatInr;

  constructor() {
    this.load();
  }

  payable(): number {
    return this.invoices()
      .filter((inv) => inv.status !== 'PAID')
      .reduce((sum, inv) => sum + Number(inv.totalAmount), 0);
  }

  overdueCount(): number {
    return this.invoices().filter((inv) => inv.daysOverdue > 0).length;
  }

  openCount(): number {
    return this.invoices().filter((inv) => inv.status !== 'PAID').length;
  }

  private load(): void {
    this.api.listMyBuyerInvoices().subscribe({
      next: (invoices) => {
        this.invoices.set(invoices);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
