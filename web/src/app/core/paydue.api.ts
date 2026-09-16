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

  listLinkedBuyers(supplierId: number) {
    return this.http.get<Buyer[]>(`/api/suppliers/${supplierId}/buyers`);
  }

  listSupplierInvoices(supplierId: number) {
    return this.http.get<Invoice[]>(`/api/suppliers/${supplierId}/invoices`);
  }

  getBuyer(buyerId: number) {
    return this.http.get<Buyer>(`/api/buyers/${buyerId}`);
  }

  listBuyerInvoices(buyerId: number) {
    return this.http.get<Invoice[]>(`/api/buyers/${buyerId}/invoices`);
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
