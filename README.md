# Garbo Backend

Spring Boot backend for the Garbo Smart Waste Management System.

## Overview

This service provides:

- Authentication and role-based authorization (JWT + Spring Security)
- Collection request and offer lifecycle APIs
- Third-party collector workflows (feed, my offers, hide/clear rejected)
- Backend-managed Cloudinary image uploads
- Database schema management with Flyway migrations

## Tech Stack

- Java 21+
- Spring Boot 3.2.x
- Spring Security (JWT)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Flyway
- Maven

## Project Structure

```text
src/main/java/com/garbo/
├── api/
│   ├── controller/               # REST endpoints
│   └── exception/                # API exception handling
├── common/
│   └── config/                   # General app configs
├── core/
│   ├── dto/                      # DTOs + API response wrappers
│   │   └── collection/
│   ├── entity/                   # JPA entities
│   ├── enums/                    # Domain enums
│   ├── repository/               # Spring Data repositories
│   └── service/                  # Business logic
├── infrastructure/
│   ├── config/
│   │   └── security/             # SecurityConfig, JWT filter/util, user details
│   └── storage/                  # Cloudinary upload service
└── Main.java                     # Application entrypoint

src/main/resources/
├── application.yml               # Main runtime configuration
└── db/migration/                 # Flyway SQL migration scripts
```

## Team Setup (First Time)

1. Create a local PostgreSQL database.
2. Configure DB values in `application.yml` (or your local profile).
3. Create `.env` from template:

```bash
cp .env.example .env
```

4. Add Cloudinary credentials in `.env`:

```env
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

## Run the Backend

Recommended local start (loads `.env` automatically):

```bash
./run-local.sh
```

Alternative manual start:

```bash
mvn spring-boot:run
```

Default API URL:

```text
http://127.0.0.1:8081
```

## Flyway Migrations

Migration location:

```text
src/main/resources/db/migration/
```

Current migrations:

- `V1__create_collection_request_module.sql`
- `V2__add_collector_hidden_to_offers.sql`

### How Flyway Applies Migrations

On application startup:

1. Flyway checks the `flyway_schema_history` table.
2. New migration files not yet recorded are applied in version order.
3. Applied versions are recorded and will not run again on the same DB.

### What V2 Adds

`V2__add_collector_hidden_to_offers.sql` adds persistence for collector-side hide/clear features:

- `collection_offers.collector_hidden BOOLEAN NOT NULL DEFAULT FALSE`
- `collection_offers.collector_hidden_at TIMESTAMP WITH TIME ZONE`
- `idx_co_collector_hidden` index for efficient collector filtered reads

### Verify Migration State

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Important Team Rules

- Never edit an already-applied migration file.
- Create a new file for every schema change:

```text
V<number>__short_description.sql
```

- Keep one logical schema change per migration.
- Keep seed/test users in seeder logic, not Flyway scripts.

## Security Notes

- Authentication is JWT Bearer token-based.
- `SecurityConfig` enforces authentication for all non-auth endpoints.
- `JwtAuthenticationFilter` skips `/api/auth/**` and validates Bearer tokens for other routes.

If you get 401 unexpectedly after adding endpoints, restart the backend to ensure latest controller mappings are loaded.

## Useful Commands

Compile:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```

Check listening port:

```bash
lsof -iTCP:8081 -sTCP:LISTEN -n -P
```
