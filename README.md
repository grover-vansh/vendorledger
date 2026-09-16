# PayDue

MSME overdue-invoice tracker. Postgres + Spring Boot.

## Run

1. Start Postgres:

```bash
docker compose up -d
```

2. Open `backend` in IntelliJ (or any IDE with Maven + **JDK 17**) and run `PaydueApplication`.

3. APIs:
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Contract file: `openapi.yaml` (project root)

## Auth

Roles: `BUYER`, `SELLER`, `ADMIN`.

Register a seller (also creates their supplier company):

```http
POST http://localhost:8080/api/auth/register
{ "email": "seller@abc.com", "password": "secret123", "role": "SELLER", "name": "ABC Foods" }
```

Login:

```http
POST http://localhost:8080/api/auth/login
{ "email": "seller@abc.com", "password": "secret123" }
```

Use `Authorization: Bearer <token>` on all other `/api` calls.
`GET /api/auth/me` returns the current user.

## UI (Angular)

From `web/`:

```bash
npm install
npm start
```

Open http://localhost:4200. The dev server proxies `/api` to Spring Boot on port 8080.

- `/login` and `/register` (roles: buyer, seller, admin)
- After login: `/seller`, `/buyer`, or `/admin`

