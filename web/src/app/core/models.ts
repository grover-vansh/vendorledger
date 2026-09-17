export type UserRole = 'BUYER' | 'SELLER' | 'ADMIN';

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
  supplierId: number | null;
  buyerId: number | null;
  companyName: string | null;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: UserRole;
  name?: string;
}

export interface Supplier {
  id: number;
  name: string;
  email: string | null;
  gstin: string | null;
  msme: boolean;
  registeredAt: string;
  productCount: number;
}

export interface Buyer {
  id: number;
  supplierId: number;
  name: string;
  email: string | null;
  gstin: string | null;
  phone: string | null;
  contactName: string | null;
  billingAddress: string | null;
  registeredAt: string;
}

export interface Product {
  id: number;
  supplierId: number;
  skuCode: string;
  name: string;
  productType: string;
  shelfLifeDays: number | null;
  active: boolean;
  registeredAt: string;
}

export interface InvoiceLine {
  id: number;
  productId: number | null;
  skuCode: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Invoice {
  id: number;
  supplierId: number;
  supplierName: string;
  buyerId: number;
  buyerName: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  status: string;
  totalAmount: number;
  daysOverdue: number;
  lines: InvoiceLine[];
}

export interface UserSummary {
  userId: number;
  email: string;
  role: UserRole;
  supplierId: number | null;
  buyerId: number | null;
  companyName: string | null;
}

export function homePath(role: UserRole): string {
  if (role === 'SELLER') {
    return '/seller';
  }
  if (role === 'BUYER') {
    return '/buyer';
  }
  return '/admin';
}

export function roleLabel(role: UserRole): string {
  if (role === 'SELLER') {
    return 'Seller';
  }
  if (role === 'BUYER') {
    return 'Buyer';
  }
  return 'Admin';
}
