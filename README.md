# Garbo Backend (`Garbo_backend`)

Spring Boot backend for the Garbo Smart Waste Management System.

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
│   ├── controller/                 # REST endpoints
│   └── websocket/                  # raw websocket handler
├── common/
│   └── config/                     # cross-cutting config (CORS, etc.)
├── core/
│   ├── dto/                        # DTOs + response wrappers
│   ├── entity/                     # JPA entities
│   ├── enums/                      # domain enums
│   ├── repository/                 # repositories (JPQL/native queries)
│   └── service/                    # business logic
├── domain/                         # OSRM/OR-Tools wrappers
├── infrastructure/
│   ├── config/
│   │   ├── security/               # SecurityConfig, JWT filter/util, UserDetails
│   │   └── StompWebSocketConfig    # STOMP broker config
│   └── websocket/                  # broadcasters/session management
└── Main.java                       # application entrypoint

src/main/resources/
└── application.yml                 # runtime configuration
```

## Prerequisites

- **Java 17** installed (and `JAVA_HOME` pointing to it)
- **Maven** (`mvn`) available
- PostgreSQL instance available (local or remote)

## Configure database

The project currently uses `application.yml` for datasource settings. Ensure the following are correct:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Hibernate setting:

- `spring.jpa.hibernate.ddl-auto: update` (auto-updates schema on startup)

## Run backend

From `Garbo_backend/`:

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
