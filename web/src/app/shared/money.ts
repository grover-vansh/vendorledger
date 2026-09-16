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
