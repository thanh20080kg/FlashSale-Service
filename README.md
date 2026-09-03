# FlashSale Service

## 1. Business

### 1.1 Overview

FlashSale Service is a Java/Spring Boot backend for running flash-sale campaigns across multiple time slots during a
day.

Flash-sale configuration is stored in MySQL. Each slot can contain multiple products with a sale price (`amount`) and a
limited quantity. The system provides authentication, OTP verification, flash-sale browsing, purchasing, payment
processing, inventory reservation, and status reconciliation.

### 1.2 Business Assumptions

- The daily purchase limit is configurable in the database. The default is `1`, but an account may purchase one or more
  products when the configured limit allows it.
- A purchase follows this sequence:

  ```text
  Hold balance -> Reserve inventory -> Commit balance -> Commit inventory
  ```

- FlashSale creates a `PENDING` purchase, requests a balance hold from Payment, reserves inventory in Warehouse,
  atomically decreases the database quota, confirms the payment, and marks the inventory as sold.
- A user cannot create another purchase while an earlier purchase is still pending. Pending purchases are handled by
  scheduled reconciliation, which either completes the transaction or rolls it back.
- The services are designed to support multiple instances. Shared state is stored in MySQL, Redis, Kafka, and RabbitMQ;
  business state is not kept in local instance memory.

### 1.3 Technical Assumptions

- Users are assumed to have a pre-funded balance. The current implementation does not validate the balance in the
  Authentication/Customer database.
- Payment balance operations are handled by the Payment service and its configured payment accounts.
- Each service is treated as an independent system with its own database/schema.
- Inbox/Outbox patterns can be added later when stronger cross-service message delivery and deduplication guarantees are
  required.

## 2. Run the Project

### 2.1 Run the Complete Stack with Docker Compose

From the repository root:

```bash
docker compose up -d --build
```

Default service endpoints:

| Service        | URL                     |
|----------------|-------------------------|
| Authentication | `http://localhost:8081` |
| FlashSale      | `http://localhost:8080` |
| Notification   | `http://localhost:8082` |
| Payment        | `http://localhost:8083` |
| Warehouse      | `http://localhost:8084` |
| Scheduler      | `http://localhost:8086` |

Check the application health:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health
```

### 2.2 Run Infrastructure in Docker and Applications Locally

Start only the infrastructure services:

```bash
docker compose up -d mysql redis rabbitmq kafka
```

Run each module from its own directory. On Windows PowerShell:

```powershell
cd Authentication
.\mvnw.cmd spring-boot:run
```

Repeat for `Notification`, `Payment`, `Warehouse`, `FlashSale`, and `Scheduler`.

Default infrastructure endpoints:

```text
MySQL:    localhost:3306
Redis:    localhost:6379
RabbitMQ: localhost:5672
Kafka:    localhost:29092
```

### 2.3 MySQL and Redis Connections

Default MySQL bootstrap administrator:

```text
Host:     localhost
Port:     3306
Username: flashsale_admin
Password: P@ssword
```

Example JDBC URL:

```text
jdbc:mysql://localhost:3306/flashsale?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=Asia/Ho_Chi_Minh
```

The initialization script creates these databases:

```text
authentication, notification, flashsale, payment, warehouse
```

Redis:

```text
redis://localhost:6379
```

Compose defaults can be overridden with environment variables such as `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`,
`MYSQL_PASSWORD`, `*_DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and the service port variables.

## 2.4 Authentication Rate Limiting

Authentication includes a Redis-based rate-limit filter to reduce brute-force attempts and OTP spam. The filter applies
to:

| Endpoint                       | Default limit |   Window | Key scope |
|--------------------------------|--------------:|---------:|-----------|
| `POST /api/v1/auth/login`      |             5 | 1 minute | Client IP |
| `POST /api/v1/auth/register`   |             5 | 1 minute | Client IP |
| `POST /api/v1/auth/verify-otp` |             5 | 1 minute | Client IP |

The service also applies identifier-based limits inside the authentication service:

| Operation        | Default limit |     Window |
|------------------|--------------:|-----------:|
| Login            |            25 | 15 minutes |
| Register         |            10 |     1 hour |
| OTP verification |            25 | 15 minutes |

Counters use atomic Redis increments and expire automatically. The filter reads `X-Forwarded-For` when the service runs
behind a trusted reverse proxy; otherwise it uses the remote address. Rate limiting can be disabled for local testing
with:

```text
RATE_LIMIT_ENABLED=false
```

If Redis is temporarily unavailable, the limiter fails open and allows the request while logging the infrastructure
error. This keeps authentication available but should be monitored in production.

## 3. APIs and Communication

The repository includes a ready-to-use Postman collection:

```text
APAGI.postman_collection.json
```

Import it into Postman to test the APIs. The service communication flow is:

```text
Client -> Authentication --Kafka--> Notification -> mock delivery/log

Client -> FlashSale --RabbitMQ request/reply--> Payment
                    --RabbitMQ request/reply--> Warehouse

Scheduler --Kafka triggers--> FlashSale and Warehouse
```

### 3.1 Register

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"identifier":"user@example.com","password":"secret123"}'
```

Authentication normalizes and validates the email or phone number, hashes the password with BCrypt, stores temporary
registration data in Redis, creates an OTP challenge in MySQL, and publishes a notification request through Kafka.
Notification renders the configured template and writes the mock delivery to its log.

### 3.2 Verify OTP

```bash
curl -X POST http://localhost:8081/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"identifier":"user@example.com","otp":"123456"}'
```

Authentication validates and consumes the OTP, creates the verified user and customer, and returns a Bearer access
token. The OTP is available in the application log for local testing.

### 3.3 Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"user@example.com","password":"secret123"}'
```

The signed token is stored as an active session in Redis.

### 3.4 Logout

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Logout removes the Redis session and invalidates the token immediately.

### 3.5 Get Current Flash-Sale Items

```bash
curl http://localhost:8080/api/v1/flash-sales/current-flashSale
```

FlashSale returns active products, sale prices, configured quantity, remaining quota, and time-slot information. The
result is briefly cached in Redis.

### 3.6 Purchase a Flash-Sale Product

```bash
curl -X POST http://localhost:8080/api/v1/flash-sales/purchase \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"itemId":"<FLASH_SALE_ITEM_ID>"}'
```

Optional quantity:

```json
{
  "itemId": "<FLASH_SALE_ITEM_ID>",
  "quantity": 1
}
```

Purchase flow:

```text
1. Validate the daily limit and Redis quota
2. Validate the active flash-sale time slot
3. Create a PENDING purchase
4. FlashSale -> Payment: hold balance
5. FlashSale -> Warehouse: reserve inventory
6. Atomically decrease the database quota
7. FlashSale -> Payment: confirm/capture payment
8. FlashSale -> Warehouse: mark the reservation as SOLD
```

Atomic database updates, Redis caching, unique reservation keys, and scheduled status synchronization are used to
prevent overselling and duplicate processing. The database quota is the source of truth; Redis is used for cached
listing data and quota-cache reloads, not as the final purchase authority.

### 3.7 Get Purchase History

```bash
curl "http://localhost:8080/api/v1/me/purchases?limit=20" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Returns the authenticated user's purchase history and purchase status.

## 4. Scheduled Jobs

Scheduler is a separate service. Cron expressions are configured in `Scheduler/src/main/resources/application.yaml`.
Scheduler publishes Kafka trigger messages; the target services consume those messages and execute the jobs.

The synchronization age is configurable. The default is `5m` for both FlashSale payment reconciliation and Warehouse
inventory reconciliation.

### 4.1 FlashSale Quota Reload

FlashSale loads active items, creates the daily quota row when needed, and reloads the Redis quota cache during
application startup. The quota-reload Kafka listener is currently disabled in `KafkaEvenListener`, so the quota-reload
topic does not trigger a runtime reload at present.

### 4.2 FlashSale Payment-Status Synchronization

The scheduler triggers FlashSale to inspect pending purchases older than `PAYMENT_SYNC_AGE` (default `5m`). FlashSale
queries Payment and, for completed payments, marks the purchase successful and finalizes Warehouse; for failed or
cancelled payments, it marks the purchase failed, restores the quota, and releases Warehouse inventory. If Payment still
returns `PENDING`, the current implementation leaves the purchase unchanged for a later synchronization cycle.

### 4.3 Warehouse Purchase-Status Synchronization

Warehouse inspects `RESERVED` reservations older than `WAREHOUSE_STATUS_SYNC_AGE` (default `5m`), requests purchase
statuses from FlashSale, and marks reservations as `SOLD` or releases them when the purchase succeeds or fails.

### 4.4 Default Kafka Topics

```text
flashsale.quota.reload
payment.status.sync
warehouse.purchase-status.sync
```

Cron schedules and topic names can be overridden through the module configuration and Docker Compose environment
variables.