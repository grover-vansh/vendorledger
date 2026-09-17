import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Buyer, Invoice, Product, Supplier, UserSummary } from './models';

@Injectable({ providedIn: 'root' })
export class PaydueApi {
  constructor(private readonly http: HttpClient) {}

  getSupplier(supplierId: number) {
    return this.http.get<Supplier>(`/api/suppliers/${supplierId}`);
  }

  listProducts(supplierId: number) {
    return this.http.get<Product[]>(`/api/suppliers/${supplierId}/products`);
  }

  listMyBuyers(supplierId: number) {
    return this.http.get<Buyer[]>(`/api/suppliers/${supplierId}/buyers`);
  }

  createBuyer(
    supplierId: number,
    body: {
      name: string;
      email: string;
      gstin?: string | null;
      phone?: string | null;
      contactName?: string | null;
      billingAddress?: string | null;
    }
  ) {
    return this.http.post<Buyer>(`/api/suppliers/${supplierId}/buyers`, body);
  }

  listSupplierInvoices(supplierId: number) {
    return this.http.get<Invoice[]>(`/api/suppliers/${supplierId}/invoices`);
  }

  createProduct(
    supplierId: number,
    body: { skuCode: string; name: string; productType?: string; shelfLifeDays?: number | null }
  ) {
    return this.http.post<Product>(`/api/suppliers/${supplierId}/products`, body);
  }

  createInvoice(body: {
    supplierId: number;
    buyerId: number;
    invoiceNumber: string;
    invoiceDate: string;
    dueDate?: string;
    lines: { productId: number; quantity: number; unitPrice: number }[];
  }) {
    return this.http.post<Invoice>('/api/invoices', body);
  }

  listMyBuyerInvoices() {
    return this.http.get<Invoice[]>('/api/buyer/invoices');
  }

  listSuppliers() {
    return this.http.get<Supplier[]>('/api/suppliers');
  }

  listUsers() {
    return this.http.get<UserSummary[]>('/api/admin/users');
  }

  createSupplier(body: { name: string; email?: string; gstin?: string; msme?: boolean }) {
    return this.http.post<Supplier>('/api/suppliers', body);
  }
}
