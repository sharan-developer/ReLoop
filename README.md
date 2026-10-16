# ReLoop – Product Afterlife & Recovery Network

Full-stack app: **React 18 + Tailwind** frontend, **Spring Boot 3.2 / Java 17+ / MySQL 8** backend.
Customers buy new/refurbished electronics, register products they own, get a **Product Lifecycle Passport**,
book repairs with partners, resell through a marketplace with safe ownership transfer, and record recycling.

---
## ⚠️ Verification status (read first)

| Part | Status |
|------|--------|
| Frontend production build (`npm run build`) | ✅ **Run and passing** |
| Frontend tests (`npm test`, 16 tests) | ✅ **Run and passing** |
| Backend source syntax | ✅ Parsed with `javac` (no syntax errors) |
| Backend **compile** (`mvn`), **tests**, and running against MySQL | ❌ **NOT run.** The build sandbox could not reach Maven Central or a MySQL server |
| End-to-end flow in a browser | ❌ Not run |

The backend (≈90 Java files) and its tests (`backend/src/test`) were written carefully but **never compiled or executed**.
Expect to fix a few compile errors or mapping mistakes on first `mvn test`; I can help from the error output.
The reference screenshot was **not attached** to the original request, so the UI follows the written description
(white header, category row, dark hero, soft-green cards), not a pixel-matched copy.

---
## 1. Prerequisites (Windows)
- JDK 17 or 21, Maven 3.9+ (or IntelliJ's bundled Maven), Node.js 18+ / npm 9+, MySQL 8.
- IntelliJ IDEA: enable **Lombok** annotation processing (Settings → Build → Compiler → Annotation Processors).

## 2. Create the database
```sql
CREATE DATABASE reloop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
(The default JDBC URL also has `createDatabaseIfNotExist=true`.)

## 3. Configuration
Copy values from `.env.example`. Spring Boot does not auto-load `.env`; set environment variables instead.

| Variable | Maps to | Notes |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `spring.datasource.*` | **Set `DB_PASSWORD`** |
| `JWT_SECRET` | `reloop.jwt.secret` | ≥32 chars; blank = random per start |
| `COOKIE_SECURE` | `reloop.cookie-secure` | `true` on HTTPS |
| `CORS_ORIGINS` | `reloop.cors-origins` | comma separated |
| `RELOOP_ADMIN_EMAIL/PASSWORD` | `reloop.admin.*` | bootstraps first admin |
| `SEED_DEMO_DATA` | `reloop.seed-demo-data` | **false in production** |
| `CLOUDINARY_*`, `RAZORPAY_*` | `reloop.cloudinary.*`, `reloop.razorpay.*` | optional |

PowerShell example:
```powershell
$env:DB_PASSWORD="your-mysql-password"
$env:JWT_SECRET="a-long-random-string-of-at-least-32-characters"
```

## 4. Backend (IntelliJ IDEA)
1. *File → Open* → select `ReLoop/backend/pom.xml` → *Open as Project*; let Maven import.
2. Open `ReLoopApplication`, *Edit Configurations* → add the environment variables above.
3. Run `ReLoopApplication`. Flyway creates the schema and catalog; the API listens on **http://localhost:8080**.

Command line: `cd backend && mvn spring-boot:run`

## 5. Frontend
```powershell
cd frontend
npm install
npm run dev      # http://localhost:5173 (proxies /api and /uploads to :8080)
```
Production build: `npm run build` (output in `frontend/dist`).

## 6–7. Seeding and running everything
Start MySQL → start backend → start frontend → open http://localhost:5173. The homepage is populated from MySQL
(17 products) on first run.

## 8. Demo accounts (local development only, created when `SEED_DEMO_DATA=true`)
| Role | Email | Password |
|---|---|---|
| Customer (owns products, has a listing) | `customer@reloop.local` | `Customer@123` |
| Second customer (buyer) | `buyer@reloop.local` | `Buyer@1234` |
| Repair partner | `partner@reloop.local` | `Partner@123` |
| Admin (only if no admin configured) | `admin@reloop.local` | `Admin@12345` |

For real deployments set `SEED_DEMO_DATA=false` and use `RELOOP_ADMIN_EMAIL/PASSWORD` for the first admin.
Public registration always creates a `CUSTOMER`. Customers can *apply* as partners (My Account); only an admin approval
grants the `REPAIR_PARTNER` role.

## 9. Swagger UI
http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`). Use *Authorize* with the access token from `/api/auth/login`.

## 10. Optional integrations
- **Cloudinary:** set the three `CLOUDINARY_*` vars. Public images (listing/repair photos) then upload there; otherwise they go to `./uploads/public`. Receipts are *always* stored privately on local disk and served only to the owner.
- **Razorpay:** set `RAZORPAY_KEY_ID/SECRET` (test keys). Orders are created server-side; the backend verifies the HMAC signature before marking anything paid. Without keys the “Pay online” option is disabled and COD works.

## 11. Common problems
| Symptom | Fix |
|---|---|
| `Access denied for user 'root'` | Set `DB_USERNAME`/`DB_PASSWORD` |
| `Cannot resolve symbol getX` / `@RequiredArgsConstructor` in IDE | Enable Lombok plugin + annotation processing |
| Flyway checksum / failed migration | Drop and recreate the `reloop` database (development only) |
| Frontend shows “Cannot reach the server” | Backend not running on :8080 |
| Logged out after backend restart | Set a fixed `JWT_SECRET` |
| Port 8080/5173 busy | Change `server.port` / `server.port` in `vite.config.js` |

## 12. Tests
```powershell
cd frontend && npm test          # 16 tests (Vitest + Testing Library)
cd backend  && mvn test          # JUnit/MockMvc on H2 (MySQL mode); not yet executed, see status above
```
Backend tests cover registration/login/role enforcement, search, cart pricing & stock, checkout + stock restore,
order isolation, passport privacy, the full repair workflow incl. partner assignment, resale + ownership transfer,
and a 6-thread concurrent purchase race.

---
## Design notes
**Token storage:** short-lived (15 min) access JWT kept in JS memory only; refresh JWT in an HttpOnly, `SameSite=Strict`
cookie scoped to `/api/auth`. This avoids localStorage XSS theft and survives reloads (`/api/auth/refresh` on startup).
Roles are always re-read from the database, never from the token or the client.

**Concurrency:** inventory uses an atomic `UPDATE … SET quantity = quantity - n WHERE quantity >= n`; resale reservation uses
`UPDATE … SET status='RESERVED' WHERE status='PUBLISHED' AND seller <> buyer` and checks affected rows (one winner).
(I used conditional updates rather than `@Version`; they are simpler and race-free here.)

**Ownership transfer:** `buy` only *reserves*. Ownership moves in a single transaction (`ResaleService.completeTransfer`) after
(a) server-verified Razorpay payment, or (b) the **seller confirms handover** for COD. The transfer writes an
`ownership_transfers` row, a lifecycle event, marks the listing SOLD, and clears the previous owner’s private receipt.

**Lifecycle integrity:** `lifecycle_events` is append-only (no update/delete code path). Each event carries a verification
level: `SELF_REPORTED`, `PARTNER_CONFIRMED` (repair start/completion by the assigned partner) or `ADMIN_CONFIRMED`.
Public passports and marketplace buyers see event types/levels only; no free text, owner identity, serial or receipt.

**Key relationships:** `users 1─* addresses/orders/owned_products`; `categories 1─* products 1─1 inventory`;
`orders 1─* order_items` (name/price snapshot; optional `listing_id` for marketplace); `owned_products 1─* warranties,
lifecycle_events, repair_requests`; `repair_requests 1─* repair_quotations, repair_status_history`;
`owned_products 1─* resale_listings`; `orders 1─1 ownership_transfers`; `reviews ─ order_items` (unique ⇒ one review per purchase).

## Known limitations / honest notes
- Backend unverified at runtime (see top). No frontend test for checkout→Razorpay or admin pages.
- Stock for an unpaid Razorpay order stays reserved until the buyer pays or cancels (no expiry job yet).
- “Mark sold” on a listing means sold *outside* ReLoop and does **not** transfer ownership.
- Sales/trend charts aggregate in Java for the last 6 months (fine for demo scale).
- Email (Spring Mail) was not added since no email configuration is part of the spec’d flows.
- Online refunds are not automated; paid order cancellation restores stock but refunds need manual handling.
