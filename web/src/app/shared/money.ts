export function roundMoney(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Math.round(value * 100) / 100;
}

export function lineTotal(quantity: number | null | undefined, unitPrice: number | null | undefined): number {
  const qty = Number(quantity ?? 0);
  const price = Number(unitPrice ?? 0);
  if (!Number.isFinite(qty) || !Number.isFinite(price) || qty <= 0 || price < 0) {
    return 0;
  }
  return roundMoney(qty * price);
}

export function formatInr(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  }).format(Number.isFinite(amount) ? amount : 0);
}

export function statusClass(status: string, daysOverdue: number): string {
  if (daysOverdue > 0 || status === 'OVERDUE') {
    return 'is-overdue';
  }
  if (status === 'PAID') {
    return 'is-paid';
  }
  if (status === 'DUE_SOON') {
    return 'is-soon';
  }
  return 'is-open';
}
