# AI Marketplace

Freelance marketplace with jobs, escrow-style payments, real-time chat, and Spring AI assists. Weeks 1–5 cover auth, jobs, proposals, orders, dual payment gateways (Razorpay + Stripe), and WebSocket chat with Redis presence.

## Stack

- Java 17, Spring Boot 3.3, Maven
- Angular 19
- MySQL 8, Redis 7
- JWT, Flyway, MapStruct, Lombok
- Payments: Razorpay + Stripe (strategy/adapters), local SIMULATED mode

## Quick start

### Infrastructure

```bash
docker compose up -d
```

Compose exposes MySQL on `localhost:3307` (`ai_marketplace`, user `aim`, password `aim123`) and Redis on `localhost:6379`.

For Compose, start the backend with:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3307/ai_marketplace?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="aim"
$env:DB_PASSWORD="aim123"
mvn -f backend/pom.xml spring-boot:run
```

Without those variables, the backend uses local MySQL on port `3306`.

### Payment keys (optional for local demo)

```powershell
$env:RAZORPAY_KEY_ID="rzp_test_..."
$env:RAZORPAY_KEY_SECRET="..."
$env:RAZORPAY_WEBHOOK_SECRET="..."
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:STRIPE_PUBLISHABLE_KEY="pk_test_..."
$env:STRIPE_WEBHOOK_SECRET="whsec_..."
$env:PAYMENTS_ALLOW_SIMULATE="true"
```

With no provider keys, use provider `SIMULATED` on the order page (enabled when `PAYMENTS_ALLOW_SIMULATE=true`).

**How wallets map**

| Method | Goes through |
|---|---|
| GPay / PhonePe / Paytm (UPI) | Razorpay Checkout |
| Paytm wallet / other Razorpay wallets | Razorpay |
| Apple Pay / Google Pay / Link | Stripe Checkout |
| Cards | Either provider |

### Frontend

```bash
cd frontend
npm install
npm start
```

- App: http://localhost:4200
- API: matches `server.port` in `backend/src/main/resources/application.yml` (currently `8082`)
- Swagger: `/swagger-ui.html`

## Week 1 APIs

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | JWT |
| GET, PUT | `/api/profiles/me` | JWT |
| GET | `/api/skills` | JWT |
| PUT | `/api/profiles/me/skills` | JWT |

Users may register as `CLIENT` or `FREELANCER`; `ADMIN` cannot be self-assigned.

## Week 2 APIs

| Method | Path | Auth |
|---|---|---|
| GET | `/api/categories` | JWT |
| POST | `/api/jobs` | CLIENT |
| GET | `/api/jobs?q&skillId&categoryId&status&page&size` | JWT |
| GET | `/api/jobs/mine?page&size` | CLIENT |
| GET | `/api/jobs/{id}` | JWT |
| PUT | `/api/jobs/{id}` | CLIENT owner |
| POST | `/api/jobs/{id}/cancel` | CLIENT owner |

## Week 3 APIs

| Method | Path | Auth |
|---|---|---|
| POST | `/api/jobs/{id}/proposals` | FREELANCER |
| GET | `/api/jobs/{id}/proposals` | CLIENT owner |
| GET | `/api/proposals/mine` | FREELANCER |
| POST | `/api/proposals/{id}/accept` | CLIENT owner |
| GET | `/api/orders/mine` | JWT (participant) |
| GET | `/api/orders/{id}` | JWT (participant) |

Accepting a proposal marks it `ACCEPTED`, rejects other open proposals, sets the job `IN_PROGRESS`, and creates an order in `AWAITING_PAYMENT`.

## Week 4 APIs

| Method | Path | Auth |
|---|---|---|
| GET | `/api/payments/methods` | JWT |
| POST | `/api/orders/{id}/pay` | CLIENT owner |
| POST | `/api/orders/{id}/pay/simulate` | CLIENT owner (local) |
| POST | `/api/payments/webhook/{razorpay\|stripe}` | Public (signature verified) |

Pay body example: `{ "provider": "RAZORPAY", "preferredMethod": "UPI" }`.  
Success marks payment `SUCCESS`, order `PAID_ESCROW`, and writes ledger `ESCROW_HOLD` + `PLATFORM_FEE`. Webhooks are idempotent via `payment_webhook_events`.

## Week 5 APIs / WebSocket

| Method | Path | Auth |
|---|---|---|
| GET | `/api/chat/orders/{orderId}` | JWT (order participant) |
| GET | `/api/chat/orders/{orderId}/messages?page&size` | JWT (participant) |
| POST | `/api/chat/orders/{orderId}/messages` | JWT (participant) |
| POST | `/api/chat/messages/{id}/read` | JWT (participant) |
| POST | `/api/chat/orders/{orderId}/read` | JWT (participant) |
| GET | `/api/chat/orders/{orderId}/presence` | JWT (participant) |
| WS | `/ws` (SockJS + STOMP) | JWT on CONNECT (`Authorization: Bearer …`) |

STOMP destinations:

- Send: `/app/chat.send` with `{ "orderId", "body" }`
- Heartbeat: `/app/presence.heartbeat`
- Subscribe: `/topic/orders.{orderId}.chat`

Chat unlocks after `PAID_ESCROW`. `OrderPaidEscrow` creates the conversation + participants + a system message. Presence keys live in Redis as `presence:{userId}` (TTL ~45s) when `CHAT_REDIS_ENABLED=true`. Message fan-out uses Redis pub/sub channel `chat:messages` in that mode; otherwise a local in-memory presence + direct STOMP fan-out is used (fine for single-instance demos).

```powershell
$env:CHAT_REDIS_ENABLED="true"   # requires Redis on localhost:6379
```

## Architecture

```text
Controller → UseCase.execute() → ApplicationService (@Transactional) → Repository
PaymentGateway (port) → Razorpay / Stripe / Simulated adapters
Chat: REST history + STOMP over `/ws`; Redis presence + pub/sub fan-out
```

- Validation belongs in use cases through `ValidationUtil`.
- `@Logging` belongs on use-case entry points.
- API boundaries use DTOs and MapStruct; entities are never returned.
- Shared errors include `message`, `details`, and `status`.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full checklist.

Use environment variables for deployed DB credentials and `JWT_SECRET`; do not commit secrets.
