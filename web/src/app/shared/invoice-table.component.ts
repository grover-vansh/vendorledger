import { Component, Input } from '@angular/core';
import { Invoice } from '../core/models';
import { formatInr, statusClass } from './money';

@Component({
  selector: 'app-invoice-table',
  templateUrl: './invoice-table.component.html'
})
export class InvoiceTableComponent {
  @Input({ required: true }) invoices: Invoice[] = [];
  @Input() party: 'buyer' | 'supplier' = 'buyer';

  readonly formatInr = formatInr;
  readonly statusClass = statusClass;

  partyName(invoice: Invoice): string {
    return this.party === 'supplier' ? invoice.supplierName : invoice.buyerName;
  }

  aging(invoice: Invoice): string {
    if (invoice.daysOverdue > 0) {
      return `${invoice.daysOverdue}d overdue`;
    }
    return 'On clock';
  }
}
