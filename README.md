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