```text
src/main/java/com/garbo/
├── api/                        # ADAPTER LAYER (External Interface)
│   ├── controller/             # REST Endpoints (Auth, Bin, Route, Citizen)
│   ├── dto/                    # Request/Response objects (Data Transfer Objects)
│   ├── mapper/                 # Converters (DTO <-> Entity)
│   └── exception/              # Global Exception Handler (Custom error responses)
│
├── core/                       # DOMAIN LAYER (The Business Brain)
│   ├── model/                  # JPA Entities (Bin, User, Vehicle, Feedback)
│   ├── repository/             # Spring Data JPA Interfaces (PostgreSQL/PostGIS)
│   └── service/                # Business Logic Interfaces
│       ├── BinService.java
│       ├── RouteService.java   # Orchestrates pathfinding
│       └── PredictionService.java
│
├── domain/                     # LOGIC IMPLEMENTATION (The "Problems" Solver)
│   ├── algorithm/              # Heavy computation logic
│   │   ├── AStarOptimizer.java # Your A* Pathfinding implementation
│   │   ├── ORToolsWrapper.java # Vehicle Routing Problem (VRP) logic
│   │   └── DijkstraEngine.java
│   └── ml/                     # Machine Learning Integration
│       ├── FillLevelPredictor.java # Calls your ML model or runs regression
│       └── TrendAnalyzer.java
│
├── infrastructure/             # INFRASTRUCTURE LAYER (External Tools)
│   ├── config/                 # Security (JWT/CORS), Redis, & Database configs
│   ├── external/               # Clients for 3rd party APIs
│   │   ├── GoogleMapsClient.java
│   │   ├── AWSS3Client.java    # For photo verification storage
│   │   └── FirebaseClient.java # For Push Notifications
│   └── redis/                  # Leaderboard & Real-time caching logic
│
└── Main.java  # Main Entry Point
```

## Flyway Migrations

Flyway migration files live in:

```text
src/main/resources/db/migration/
```

Current project rule:

1. Keep schema history in backend code and commit it to Git.
2. Use Flyway for schema changes and keep Hibernate on `ddl-auto=validate`.
3. Add a new migration for every schema change.
4. Do not edit or rename an old migration after it has run on a shared database.
5. Keep demo/test users in `DataSeeder`, not in Flyway migrations.

Naming convention:

```text
V1__create_collection_request_module.sql
V2__add_collection_request_indexes.sql
V3__add_offer_completion_fields.sql
```

Guidelines:

- Use `V<number>__<short_clear_description>.sql`
- Use lowercase words with underscores
- Keep one logical schema change per file
- Prefer clear names like `create_*`, `add_*`, `alter_*`, `drop_*`

Recommended split:

- Flyway: tables, columns, indexes, constraints
- Seeder: demo users, sample records, local bootstrap data

How to inspect migration status in PostgreSQL:

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Backend-Managed Image Upload (Cloudinary)

Third-party collector completion photos are uploaded by backend, not by the Flutter app.

Set these environment variables before running backend:

```bash
export CLOUDINARY_CLOUD_NAME=your_cloud_name
export CLOUDINARY_API_KEY=your_api_key
export CLOUDINARY_API_SECRET=your_api_secret
```

Then start backend normally:

```bash
mvn spring-boot:run
```

This allows all team members to run mobile app without passing Cloudinary Dart defines.
