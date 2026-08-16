# Garbo Backend (`Garbo_backend`)

Spring Boot backend for the Garbo Smart Waste Management System.

> CD: push to `devops/platform` deploys to AWS EC2 via GitHub Actions.

## Overview

This service provides:

- **Auth**: JWT login + role-based authorization (Spring Security)
- **Admin dashboard APIs**: analytics + management endpoints
- **Collection workflows**: requests/offers lifecycle
- **Realtime**: WebSocket endpoints (raw `/ws` + STOMP `/ws-stomp`)
- **Persistence**: PostgreSQL via Spring Data JPA / Hibernate

## Tech stack

- **Java**: 17 (build is configured for Java 17)
- **Spring Boot**: 3.2.x
- **Spring Security**: JWT
- **JPA**: Hibernate
- **DB**: PostgreSQL (or H2 if you configure it)
- **Build**: Maven

## Ports and URLs (local)

- **Backend HTTP port**: `8080` (see `application.yml`)
- **Base URL**: `http://localhost:8080`

WebSocket endpoints:

- **Raw WebSocket**: `ws://localhost:8080/ws` (custom handler)
- **STOMP**: `ws://localhost:8080/ws-stomp` (used by `SimpMessagingTemplate` publishing to `/topic/**`)

## Project structure

```text
src/main/java/com/garbo/
├── api/
│   ├── controller/                     # REST endpoints
│   │   ├── citizen/                    # citizen-specific flow endpoints
│   │   ├── field_mentor/               # field staff / bin-report endpoints
│   │   ├── shared/                     # shared request/offer lifecycle endpoints
│   │   ├── third_party_collector/      # third-party collector endpoints
│   │   └── adminAnalytics/             # admin analytics endpoints
│   ├── dto/
│   │   └── common/                     # shared API response wrapper
│   ├── exception/                      # global and flow-specific exception handling
│   ├── mapper/                         # API-level mappers
│   └── websocket/                      # raw websocket handler
├── common/
│   ├── config/                         # cross-cutting config such as CORS
│   └── logging/                        # shared logging helpers
├── core/
│   ├── dto/                            # domain-facing DTOs
│   │   └── collection/                 # collection request / offer DTOs
│   ├── entity/                         # JPA entities
│   ├── enums/                          # domain enums
│   ├── repository/                     # repositories (JPQL/native queries)
│   └── service/                        # business logic
│       ├── citizen/                    # citizen-specific services
│       ├── field_staff/                # field staff / bin services
│       ├── route/                      # route session services
│       ├── shared/                     # shared collection workflow services
│       ├── third_party_collector/      # collector profile / collector-specific services
│       └── AdminAnalytics/             # admin analytics services
├── domain/                             # OSRM/OR-Tools wrappers
├── infrastructure/
│   ├── config/
│   │   └── security/                   # SecurityConfig, JWT filter/util, UserDetails
│   ├── email/                          # email integration
│   ├── storage/                        # Cloudinary upload service
│   └── websocket/                      # broadcasters/session management
└── Main.java                           # application entrypoint

src/main/resources/
├── application.yml                     # shared runtime configuration
├── application-local.yml               # local profile config
├── application-prod.yml                # prod profile config
└── db/migration/                       # SQL migrations
```

### Current organization notes

- Controllers are now grouped by flow ownership instead of keeping all scoped endpoints in one flat controller package.
- Shared collection lifecycle endpoints remain under `api/controller/shared`.
- `ApiResponse` now lives in `api/dto/common`.
- Shared collection workflow logic lives under `core/service/shared`, including the extracted `CollectorDashboardService`.

## Prerequisites

1. Create a local PostgreSQL database.
2. Create `.env` from template:

```bash
cp .env.example .env
```

3. Pick the profile in `.env`:

```env
SPRING_PROFILES_ACTIVE=local
```

Use `local` for your machine's PostgreSQL database.
Use `prod` for the remote PostgreSQL database.

4. For `local`, fill your own PostgreSQL connection values in `.env`:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/garbo_db
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DATASOURCE_USERNAME=your_local_db_username
SPRING_DATASOURCE_PASSWORD=your_local_db_password
```

5. For `prod`, fill the remote DB values in `.env`:

```env
PROD_SPRING_DATASOURCE_URL=
PROD_SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
PROD_SPRING_DATASOURCE_USERNAME=
PROD_SPRING_DATASOURCE_PASSWORD=
```

6. Add Cloudinary credentials in `.env`:

```env
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

Notes:
`src/main/resources/application-local.yml` reads `SPRING_DATASOURCE_*` values from `.env`.
`src/main/resources/application-prod.yml` reads `PROD_SPRING_DATASOURCE_*` values from `.env`.
`.env` is ignored by Git, so each team member can keep their own local DB credentials safely.

## Run the Backend

Recommended local start (loads `.env` automatically):

```bash
./run-local.sh
```

Alternative manual start:

```bash
mvn clean compile
mvn spring-boot:run
```

To run on another port temporarily:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## CORS (frontend connectivity)

The backend is configured to allow browser requests from local dev origins, including:

- `http://localhost:3000`
- `http://localhost:3001`

This is required for `fetch()` preflight (`OPTIONS`) and login calls like `POST /api/auth/login`.

If you change the frontend port, update allowed origins in `CorsConfig`.

## Authentication

### Login endpoint

- `POST /api/auth/login`

Request body:

```json
{ "email": "user@example.com", "password": "secret" }
```

Response includes:

- `token` (JWT)
- `role`
- `mustChangePassword`
- optional `council` for `AdminNew` users

## Council-scoped admin behavior

Current dashboard behavior relies on the authenticated admin council (returned by login and used in frontend requests):

- superadmin can switch councils from the dashboard
- admin users are restricted to their own council context
- admin create flows (for example bins/vehicles from dashboard) enforce the admin council in payloads

This implementation does **not** require database table changes.

## User management endpoints used by dashboard

The dashboard now uses these secured user endpoints:

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{userId}`
- `DELETE /api/users/{userId}`

Use `Authorization: Bearer <token>` for protected operations.

### Change password

- `POST /api/auth/change-password`

Request body:

```json
{ "email": "user@example.com", "oldPassword": "old", "newPassword": "new" }
```

## Backend file-change audit logging

For security and governance, backend file modification attempts are now audited.

- Audit log file (default): `logs/backend_file_audit.log`
- Event scope: sensitive local backend file write paths (upload and local fallback storage)
- Captured context: timestamp, actor, IP address, request path, target file path, outcome, detail

Override log destination if needed:

```env
AUDIT_FILE_CHANGE_LOG_PATH=logs/backend_file_audit.log
```

Or via application property:

```yaml
audit:
	file-change:
		log-path: logs/backend_file_audit.log
```

## Monitoring & Exception Tracking (Sentry & Prometheus)

### 1. Prometheus Metrics Scraping
* The backend exposes system, JVM, and application-level metrics at `/actuator/prometheus` via Micrometer.
* **Security Configuration**: Access to `/actuator/prometheus` is configured to `permitAll()` in the Spring Security filter chain to allow the Prometheus scraper to pull metrics without requiring authentication.

### 2. Sentry Integration
* **Sentry SDK**: Uses `sentry-spring-boot-starter-jakarta` for Jakarta/Spring Boot 3.x error logging.
* **Configuration**: DSN details are loaded dynamically from `/garbo/prod/sentry-dsn` in the AWS Systems Manager (SSM) Parameter Store.
* **Manual Verification**: A GET endpoint is exposed at `/api/app/test-sentry` to manually trigger an arithmetic exception and verify that errors are logged correctly in Sentry.

## Database Migrations & Flyway

This project uses Flyway to manage database schema migrations.

### Settings in `.env`
To enable Flyway migrations locally, make sure you have:
```env
SPRING_FLYWAY_ENABLED=true
```

### Safety Features Configured
Flyway is configured with the following parameters to ensure smooth integration on databases that have existing tables generated by Hibernate:
- **Baseline on Migrate** (`baseline-on-migrate: true`): Baselines existing schemas without trying to recreate existing tables from scratch.
- **Out of Order** (`out-of-order: true`): Allows applying unapplied historical migrations out of order.
- **Validate on Migrate** (`validate-on-migrate: false`): Bypasses checksum validation on existing migrations to allow safe collaboration where local files differ slightly from production history records.
- **Ignore Missing Migrations** (`ignore-migration-patterns: "*:missing"`): Safely ignores missing migration files that are already recorded in production history.

---

## Database Enforcements

### Email Uniqueness Constraint
- **Constraint**: The `email` column in the `users` table has a unique index (`uq_users_email_lower`) on `LOWER(email)` to enforce case-insensitive uniqueness.
- **Auto-Cleanup**: Historical duplicate email accounts are automatically suffix-renamed to `email+dup<emp_id>@domain` (e.g. `collector.test+dup172@garbo.com`) during the `V9` database migration.
- **Global Validation**: The application performs global email uniqueness validation against the `UserRepository` in registration flows (e.g., in `CitizenService`).

## Common troubleshooting

### Port already in use

If you see “Port 8080 was already in use”, stop the other process or change the port.

Check who is listening:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

### CORS blocked in browser

If you see “No `Access-Control-Allow-Origin` header”, make sure:

- backend is running on `8080`
- frontend origin (e.g. `http://localhost:3001`) is allowed in `CorsConfig`
- you’re calling the correct endpoint (`/api/auth/login`)

### Build fails with missing Lombok methods

The Maven compiler plugin is configured with Lombok annotation processing in `pom.xml`.
If your IDE still shows errors, enable annotation processing in the IDE settings.

## Testing

The backend has a layered test suite covering unit tests, controller slice tests, and end-to-end integration tests.

### Test stack

| Library | Purpose |
|---------|---------|
| **JUnit 5 (Jupiter)** | Test runner and assertions |
| **Mockito** | Mocking dependencies in unit tests |
| **Spring Boot Test** | `@SpringBootTest`, `@WebMvcTest`, `MockMvc` |
| **Spring Security Test** | `@WithMockUser`, CSRF helpers |
| **H2 Database** | In-memory PostgreSQL-compatible DB for integration tests |
| **Testcontainers** | Real PostgreSQL container for repository tests (requires Docker) |

### Test configuration

Tests use the `test` profile which loads `src/test/resources/application-test.yml`:

- **Database**: H2 in-memory with `MODE=PostgreSQL` (no external DB needed)
- **Flyway**: Disabled (schema created by Hibernate `create-drop`)
- **Logging**: `WARN` level only

### Test categories

#### 1. Unit tests (service layer)

Located in `src/test/java/com/garbo/core/service/`.

These test business logic in **isolation** without starting Spring. All dependencies are mocked with `@Mock` and the service under test is manually instantiated.

| Test class | What it covers |
|------------|---------------|
| `RouteAssignmentServiceUnitTest` | Bin collection idempotency, route persistence, skip/pending status |
| `RouteSessionServiceTest` | Route session optimization logic |
| `NotificationServiceTest` | Notification dispatch logic |

#### 2. Controller slice tests (API layer)

Located in `src/test/java/com/garbo/api/controller/`.

These use `@WebMvcTest` to test HTTP endpoints, request mapping, JSON serialization, and security authorization **without** starting the full application.

| Test class | What it covers |
|------------|---------------|
| `RouteControllerTest` | Route optimization endpoint success/error responses |
| `RouteSessionControllerTest` | Route session CRUD endpoints |

#### 3. Flow integration tests (end-to-end)

Located in `src/test/java/com/garbo/flow/`.

These use `@SpringBootTest` with `MockMvc` to test **complete multi-step workflows** against a real Spring context and H2 database. Entities are created, API calls are chained, and state transitions are verified at each step.

| Test class | What it covers |
|------------|---------------|
| `CitizenToCollectorFlowIT` | Full citizen ↔ collector request-offer workflow |

**`CitizenToCollectorFlowIT` test cases:**

| Test | Flow covered |
|------|-------------|
| `citizenCreatesRequest_collectorOffers_citizenAccepts` | Request → Offer → Accept (happy path) |
| `citizenRejectsOffer_requestRemainsOpen` | Reject offer without closing the request |
| `citizenCancelsRequest_rejectsPendingOffers` | Cancel request cascades to reject pending offers |
| `collectorWithdrawsOffer_statusWithdrawn` | Collector withdraws their pending offer |
| `fullLifecycle_pendingToConfirmed` | Complete lifecycle: OPEN → PENDING → ACCEPTED → IN_PROGRESS → COMPLETED → CONFIRMED |
| `acceptOffer_autoRejectsOtherPendingOffers` | Accepting one offer auto-rejects all other pending offers |

#### 4. Repository integration tests (requires Docker)

Located in `src/test/java/com/garbo/repository/`.

| Test class | What it covers |
|------------|---------------|
| `RepositoriesIntegrationTest` | Route assignment, vehicle route, and bin stop persistence against real PostgreSQL |

> **Note**: This test uses Testcontainers and requires **Docker** to be running. It will fail with `IllegalStateException: Could not find a valid Docker environment` if Docker is not available.

### Running tests

**Run all tests** (skip `RepositoriesIntegrationTest` if Docker is not available):

```bash
mvn test -Dtest='!com.garbo.repository.RepositoriesIntegrationTest'
```

**Run all tests** (including Testcontainers — requires Docker):

```bash
mvn test
```

**Run a specific test class:**

```bash
mvn test -Dtest=CitizenToCollectorFlowIT
```

**Run a single test method:**

```bash
mvn test -Dtest='CitizenToCollectorFlowIT#fullLifecycle_pendingToConfirmed'
```

**Run only unit tests** (fastest, no Spring context):

```bash
mvn test -Dtest='RouteAssignmentServiceUnitTest,NotificationServiceTest,RouteSessionServiceTest'
```

## Useful commands

Compile:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```
