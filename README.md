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

## Useful commands

Compile:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```
