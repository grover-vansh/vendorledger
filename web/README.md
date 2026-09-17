# PayDue web

Angular 19 UI for the MSME overdue-invoice tracker.

```bash
npm install
npm start
```

Then open http://localhost:4200 (or http://127.0.0.1:4200). The dev server proxies `/api` to Spring Boot on port 8080.

- `/login` and `/register` — buyer, seller, or admin
- After sign-in: `/seller`, `/buyer`, or `/admin`
- Seller actions: `/seller/buyers`, `/seller/skus`, `/seller/invoices/new`
