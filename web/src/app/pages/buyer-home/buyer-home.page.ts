import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { Buyer, Invoice } from '../../core/models';
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
  readonly buyer = signal<Buyer | null>(null);
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
    const buyerId = this.auth.user()?.buyerId;
    if (!buyerId) {
      this.loading.set(false);
      this.error.set('This buyer account is not linked to a buyer company.');
      return;
    }

    forkJoin({
      buyer: this.api.getBuyer(buyerId),
      invoices: this.api.listBuyerInvoices(buyerId)
    }).subscribe({
      next: (data) => {
        this.buyer.set(data.buyer);
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
