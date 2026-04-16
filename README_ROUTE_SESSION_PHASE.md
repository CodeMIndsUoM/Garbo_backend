# Route Session Phase Summary

## Objective
Implemented a session-based, user-targeted, real-time route optimization flow for Flutter consumers, while keeping HTTP trigger endpoints for web/admin usage.

## What Was Implemented

### 1) Real-time Infrastructure
- Added Spring WebSocket/STOMP support.
- Configured broker topics and handshake endpoint.

Files:
- src/main/java/com/garbo/common/config/WebSocketConfig.java
- pom.xml

### 2) Session-Based Route API
- Added a dedicated controller for route sessions.
- Supports create/replace, latest snapshot retrieval, recompute, and delete by session or by user.

File:
- src/main/java/com/garbo/api/controller/RouteSessionController.java

Endpoints:
- POST /api/route-sessions
- GET /api/route-sessions/{sessionId}/latest
- GET /api/route-sessions/users/{userId}/latest
- POST /api/route-sessions/{sessionId}/recompute
- POST /api/route-sessions/users/{userId}/recompute
- DELETE /api/route-sessions/{sessionId}
- DELETE /api/route-sessions/users/{userId}

### 3) Session DTO Contract
- Added create-request DTO with userId + selected bin support.
- Added create-response DTO with websocketTopic and latest snapshot.
- Added snapshot DTO with versioning and change metadata.

Files:
- src/main/java/com/garbo/api/dto/RouteSessionCreateRequestDTO.java
- src/main/java/com/garbo/api/dto/RouteSessionCreateResponseDTO.java
- src/main/java/com/garbo/api/dto/RouteSessionSnapshotDTO.java

Snapshot fields include:
- sessionId
- userId
- version
- status (PROCESSING, READY, ERROR)
- trigger
- selectedBinIds
- addedBinIds
- removedBinIds
- route

### 4) Core Session Service
- Implemented per-user active session mapping.
- Added debounce-based recomputation and async compute execution.
- Added versioned snapshot publishing.
- Added selected-bin filtering and ordered loading.
- Added diff computation for added/removed bins between versions.

File:
- src/main/java/com/garbo/core/service/route/RouteSessionService.java

Supporting state file:
- src/main/java/com/garbo/core/service/route/RouteSessionState.java

WebSocket publish topics:
- /topic/routes/users/{userId}
- /topic/route-sessions/{sessionId}

### 5) Bin Change Event Integration
- Added bin-change domain event.
- Bin create/delete/update now publish events.
- Route sessions automatically recompute when relevant bins change.

Files:
- src/main/java/com/garbo/core/service/event/BinChangedEvent.java
- src/main/java/com/garbo/core/service/BinService.java

### 6) RouteController Adapter Behavior
- Existing /api/routes/optimize endpoint now adapts the request into session flow and broadcasts snapshot output.

File:
- src/main/java/com/garbo/api/controller/RouteController.java

### 7) Route Request DTO Upgrade
- Added userId and selectedBinIds to support user-scoped optimization requests.

File:
- src/main/java/com/garbo/api/dto/RouteRequestDTO.java

## Optimization Pipeline (Current)
1. Validate request and session ownership.
2. Resolve bins (selected list or all bins).
3. Build depot + bin coordinates.
4. Fetch OSRM duration matrix.
5. Solve VRP using OR-Tools with capacity constraints.
6. Build detailed vehicle routes.
7. Publish versioned snapshot to WebSocket subscribers.

Main solver files:
- src/main/java/com/garbo/domain/OSRMClient.java
- src/main/java/com/garbo/domain/ORToolsWrapper.java

## Validation Status
- Maven compile completed successfully with current changes:
  - mvn -q -DskipTests compile

## Suggested Next Improvements
- Add authentication/authorization for user-scoped topics.
- Persist route sessions/snapshots to database (currently in-memory state).
- Replace public OSRM demo with dedicated OSRM instance for production.
- Add integration tests for session lifecycle and version progression.
