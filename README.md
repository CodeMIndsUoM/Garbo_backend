# Garbo Backend

Spring Boot backend for Garbo Smart Waste Management System.

## 1) Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+ (or compatible)

## 2) First-Time Setup (New Team Member)

1. Create a local database.
2. Copy environment template and fill values:

```bash
cp .env.example .env
```

3. Update `.env` with your real values:

```env
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

4. Ensure `application.yml` points to your local PostgreSQL instance.

## 3) Run Backend

Recommended (loads `.env` automatically):

```bash
./run-local.sh
```

Alternative:

```bash
export CLOUDINARY_CLOUD_NAME=...
export CLOUDINARY_API_KEY=...
export CLOUDINARY_API_SECRET=...
mvn spring-boot:run
```

Backend base URL (default):

```text
http://127.0.0.1:8081
```

## 4) Flyway Migration Flow

Migration files are in:

```text
src/main/resources/db/migration/
```

How migrations are applied:

1. On backend startup, Flyway checks `flyway_schema_history`.
2. Any migration file not yet recorded is executed in version order.
3. Executed migrations are recorded and will not run again.

Current relevant migrations:

- `V1__create_collection_request_module.sql`
- `V2__add_collector_hidden_to_offers.sql`

### What changed in V2

`V2__add_collector_hidden_to_offers.sql` adds persistent hide support for collector My Jobs:

- `collection_offers.collector_hidden BOOLEAN NOT NULL DEFAULT FALSE`
- `collection_offers.collector_hidden_at TIMESTAMP WITH TIME ZONE`
- index `idx_co_collector_hidden (collector_id, collector_hidden, status, created_at)`

This is the schema change used by:

- single offer remove from list
- bulk clear-all in rejected tab

### Verify migration applied

Run in PostgreSQL:

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

You should see `V2` with `success = true`.

## 5) Team Notes

- Do not edit old migration files after they are shared.
- Add a new `V<number>__description.sql` for every schema change.
- Keep `spring.jpa.hibernate.ddl-auto=validate` for migration safety.
- Seed/demo users are handled by backend seeder code, not Flyway migration files.

## 6) Useful Commands

Compile check:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```
