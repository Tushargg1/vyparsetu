# VyaparSetu

AI-powered B2B commerce platform connecting retailers and suppliers. This repo contains the **V1** implementation: a Spring Boot backend and a React frontend, organized as a modular monolith.

See the full technical design in [`.kiro/specs/vyaparsetu-v1/design.md`](.kiro/specs/vyaparsetu-v1/design.md).

## Stack

- **Backend**: Java 17, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, MySQL, Flyway, springdoc/OpenAPI.
- **Frontend**: React 19, Vite, Tailwind CSS, DaisyUI, React Router, React Query, Zustand, i18next (Hindi + English).
- **Infra**: Docker + docker-compose (MySQL, backend, frontend).

> Note: the spec targets Java 21; this build targets Java 17 to match the available toolchain. Bump `java.version` in `backend/pom.xml` to 21 when a JDK 21 is available.

## Project Structure

```
backend/    Spring Boot modular monolith (auth, user, catalog, inventory, order, payment, ai, ...)
frontend/   React 19 + Vite SPA (feature-sliced)
docker-compose.yml
```

## Modules Implemented

**Phase 1**
- **Auth & Users** — OTP-based registration/login, JWT access + rotating refresh tokens, RBAC.
- **Catalog** — products & categories, search, barcode lookup.
- **Inventory** — single-entry stock mutations with an append-only movement ledger, batches/expiry (FEFO), low-stock alerts, non-negative guarantee.
- **Orders** — cart, order placement with GST totals, order state machine, repeat order, supplier order queue.

**Phase 2**
- **Payments** — payment init/confirm (idempotent), wallet ledger, credit charging.
- **Notifications** — persisted in-app notifications + WebSocket (STOMP) push, email/SMS/push channel adapters, wired into the order lifecycle.
- **WhatsApp** — gated client (`app.features.whatsapp.enabled`, off by default — billed per message).
- **Delivery** — assignment, delivery OTP verification, proof of delivery, COD collection.
- **Invoices** — GST invoice generation (HTML via storage abstraction; S3-ready).
- **Ordering methods** — cart, repeat, barcode, and AI text/voice/image-to-order.

**Phase 3**
- **AI** — rule-based text/voice/image-to-order matching, smart reorder, **demand forecasting** (days-to-stockout + next purchase date), chat assistant (offline default, swappable `AiClient`).
- **Bulk Procurement** — campaigns + commitments, **disabled by default** behind `app.features.procurement.enabled`.

**Phase 4**
- **Reports** — retailer sales/profit/purchase/inventory, supplier sales.
- **Credit management** — supplier/admin set limits, approve retailers, per-supplier limit enforcement.
- **Admin** — platform dashboard counts, user listing, activate/suspend users.
- **Logistics** — delivery partner assignments, OTP, proof of delivery, COD (see Delivery).

Feature flags keep procurement and WhatsApp inert until enabled.

## Running Locally

### Prerequisites
- JDK 17, Maven 3.9+
- Node 20+ / npm
- MySQL 8 (or use docker-compose)

### Option A — Docker (everything)
```bash
docker compose up --build
```
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui

### Option B — Run services directly

Start MySQL (or `docker compose up mysql`), then:

```bash
# backend
cd backend
mvn spring-boot:run

# frontend (new terminal)
cd frontend
npm install
npm run dev
```

The frontend dev server proxies `/api` to `http://localhost:8080`.

## API Docs

Once the backend is running, open **http://localhost:8080/swagger-ui**. All endpoints are under `/api/v1`. Authenticate via the `bearerAuth` scheme using the access token from `/auth/otp/verify`.

### Quick auth flow (dev)
1. `POST /api/v1/auth/register` with `{ name, phone, role: "RETAILER", shopName }`.
2. The OTP is printed to the backend logs (dev OTP sender).
3. `POST /api/v1/auth/otp/verify` with `{ identifier: phone, code, purpose: "REGISTER" }` → returns tokens.

## Tests

```bash
cd backend
mvn test
```

Unit tests cover the inventory non-negative invariant and the order state machine.

## Roadmap

- Phase 2: full ordering methods (voice/image), WhatsApp integration, notifications, payment gateway.
- Phase 3: AI demand forecasting, bulk procurement.
- Phase 4: logistics, analytics, credit scoring.
