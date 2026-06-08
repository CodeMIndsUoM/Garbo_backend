# Garbo — Web Dashboard & App Update Plan

> Living document. Edit freely. Each feature has checkboxes, an owner, and an explicit
> backend / web / app split so **two developers (App + Web) can work in parallel with
> minimal merge conflicts**. The only shared surface is the **Backend (API + WebSocket
> contract)** — changes there must be agreed first (see §3).

- **Status legend:** `[ ]` todo · `[~]` in progress · `[x]` done · `[?]` needs decision
- **Owners:** **WEB** = web dashboard dev · **APP** = Flutter dev · **BE** = backend (shared, coordinate) · **AI** = clustering/optimization (lives in backend Java for now)
- Last updated: 2026-06-08

---

## 0. How to use this document

1. Read §1 (architecture snapshot) and §2 (workstream split) once.
2. Pick features from §4. Each feature is self-contained with its own backend/web/app sub-tasks.
3. Before touching the backend, read §3 (contract rules) so the two devs don't collide.
4. Tick checkboxes as you go. Add notes under each feature's **Notes** line.
5. `[?]` items need a decision from the product owner before starting — collected in §6.

---

## 1. Current architecture snapshot

| Component | Stack | Location |
|---|---|---|
| Web dashboard | Next.js (App Router shell) + React + Tailwind + Leaflet | `Garbo_web_dashboard/` |
| Backend | Spring Boot 3.2, JWT, JPA/Postgres, STOMP + raw WebSocket, OR-Tools + OSRM | `Garbo_backend/` |
| Mobile app | Flutter, `http` + `provider`, STOMP over SockJS | `Garbo-flutter/` |
| AI engine | **Empty git repo** (no code) — clustering will live in backend Java | `Garbo-AI-engine/` |

### Web (key facts)
- **No router for features.** Everything is a `PageType` string switched inside
  `Garbo_web_dashboard/src/app/page.tsx` → `renderPage()`.
- Sidebar: `Garbo_web_dashboard/src/components/Sidebar.tsx`.
- **No shared API client.** Each component re-declares `API_BASE` + `fetch` + auth headers.
- Council filter = local state `tabCouncilFilters` in `page.tsx` (per-page), plus a *separate*
  council `<select>` inside `Map.tsx`.
- Auth/session via `sessionStorage` (`token`, `role`, `council`, `mustChangePassword`).
- Dark-mode tokens exist in `globals.css` (`.dark`) + `tailwind.config.ts` (`darkMode: ['class']`)
  but are **never activated** (no toggle, no `class="dark"`).
- WebSocket is used **only** in `Map.tsx` for route-session status — **not** for bins.

### Backend (key facts)
- Roles (DB): `SUPERADMIN`, `ADMIN` (`AdminNew`, has `council`), `FIELD_MENTOR`,
  `BIN_COLLECTOR`, `CITIZEN`, `THIRD_PARTY_COLLECTOR`. (No separate `FIELD_STAFF`.)
- Internal-user creation (email + temp password + `mustChangePassword`) already implemented:
  `AdminStaffController` → `AdminStaffService.createFieldMentor/createBinCollector`,
  email via `infrastructure/email/EmailService.sendAdminCredentials`.
- WebSocket: STOMP `/ws` (route sessions, topics `/topic/routes/users/{id}`,
  `/topic/route-sessions/{id}`) **and** raw `/ws-raw` (`GarboWebSocketHandler`) for
  `BIN_STATUS_UPDATED`, `TASK_PROGRESS_UPDATE`, `LEADERBOARD_UPDATE`, `BIN_COLLECTION_ACK`.
- `Bin.zone` is a free-text `String` (default `"unassigned"`). **No clustering / K-means.**
- Route optimization = OSRM duration matrix + Google OR-Tools CVRP (`domain/OSRMClient.java`,
  `domain/ORToolsWrapper.java`), in-memory sessions + DB persistence.
- 3rd-party registration (incl. NIC photo → Cloudinary) + approve/reject endpoints exist under
  `/api/auth/thirdparty-register/**` but are **`permitAll()`** (no JWT) — security gap.
- SMTP configured in `application.properties` (Gmail). **Credentials are committed** — rotate/secret.

### App (key facts)
- Roles routed in `core/router/app_router.dart`. Login = `presentation/auth/pages/login.dart`
  → `POST /auth/login`; first-login change = `presentation/auth/pages/change_password.dart`.
- Field staff bin report + undo: real, with `BIN_STATUS_UPDATED` realtime.
- Bin collector route/collection: real, with STOMP + REST.
- Citizen ↔ 3rd-party marketplace: real but **polling-based (15s)**, no WebSocket.
- 3rd-party registration with NIC upload: real (`auth/pages/collector_register.dart`).
- Stubs (UI only, no backend): citizen `report.dart`, citizen `events.dart`,
  citizen `register.dart`, `forgot_password.dart`, 3rd-party `set-password` UI.

### Known cross-cutting issues to fix opportunistically
- [ ] **BE** `FieldMentorService.getAll()/findByCouncil()` throw `UnsupportedOperationException`.
- [ ] **BE** `Complaint` entity has stub methods throwing `UnsupportedOperationException`.
- [ ] **BE** Role-string inconsistency: DB uppercase vs JWT lowercase vs `@PreAuthorize` uppercase.
- [ ] **BE** Many endpoints `permitAll()` (bins, vehicles, routes, analytics, 3rd-party approve).
- [ ] **WEB** Bin status vocabulary mismatch (`critical/warning/normal` vs `full/half/empty`).
- [ ] **APP** Dual auth endpoints (`/auth/login` vs legacy `/api/users/login` in `AuthProvider`).
- [ ] **BE** Committed Gmail credentials in `application.properties` → move to env/secret.

---

## 2. Workstream split (parallelization)

Two developers work simultaneously. Backend is shared; sequence backend contract changes early
in each feature so the other dev can build against a stable contract.

| Track | Owner | Scope |
|---|---|---|
| **Track W (Web)** | WEB dev | Everything under `Garbo_web_dashboard/`. Owns F1, F2, F3, F4(web), F6(web), F11. |
| **Track A (App)** | APP dev | Everything under `Garbo-flutter/`. Owns F7(app), F9(app), F10(app). |
| **Track B (Backend)** | Whoever owns the feature, reviewed by both | `Garbo_backend/`. Touched by F1, F5, F6, F7, F8, F9, F10. |

**Conflict-free by construction:** Web files and Flutter files never overlap. The only shared
files are in `Garbo_backend/`. To avoid backend collisions:
- Each feature lists **exactly which backend files/classes it adds or edits**.
- Prefer **new controller/service/DTO classes** over editing shared ones.
- Whoever needs a backend change first writes the DTO/endpoint + a stub response, merges it, then
  both build against it.

**Recommended order (high level):**
1. F1 (nav/council) + F8-verify run first — low risk, unblocks everyone.
2. F9 (realtime dashboard) backend contract early — both web and app depend on it.
3. F3, F4 (web-only) and F7, F10 (app-only) run fully in parallel.
4. F5 (clustering) and F6 (external users) mid-stream.
5. F2 decision, then F11 (UI polish + dark theme) **last**.

---

## 3. Backend contract rules (read before editing `Garbo_backend/`)

1. **New endpoints get a written contract first** — add it to §5 (API/WS Contract Registry)
   in this file (method, path, request, response, auth/role) before implementing.
2. **Never rename/remove an existing field** other dev may use; add new fields additively.
3. **WebSocket message `type` strings are a shared enum** — list them in §5; don't reuse names.
4. **Council scoping** must use the existing `CouncilAccessService` / `CurrentUserService`.
5. **Security**: new admin endpoints require JWT + role check (`@PreAuthorize`). Do not add new
   `permitAll()` routes.
6. One backend PR per feature where possible; keep migrations in
   `src/main/resources/db/migration` with incremental version numbers.

---

## 4. Features

### F1 — Remove "Home", make Council selection a global dashboard control (WEB + small BE)

**Goal:** Delete the superadmin-only "Home" page. Make council filtering a single global control
that applies to every page, only visible to superadmin; council-admins are locked to their council.

**Current state:** `home` PageType = `SuperadminCouncilSelect`. Per-page council state in
`tabCouncilFilters` (page.tsx); a *second* council selector lives in `Map.tsx`. Council-admin's
council comes from `sessionStorage.council`.

**Target UX (DECIDED: single top-bar dropdown with "All Councils"):**
- Remove "Home" from sidebar and `renderPage()`; default landing = `dashboard`.
- Introduce a **global `CouncilContext`** (React context provider in `page.tsx` or a new
  `src/lib/council-context.tsx`) holding `{ selectedCouncil, setSelectedCouncil, isSuperadmin, councils }`.
- Render **one** council selector in the top bar (the existing top-bar `<select>` area), visible
  only to superadmin, persisted to `sessionStorage` so it survives page switches and reloads.
  Include an **"All Councils"** option.
- Remove the duplicate selector inside `Map.tsx`; Map consumes `CouncilContext`.
- Council-admins: hide the selector, show their fixed council name (read-only chip).

**Web tasks (Track W):**
- [ ] Create `src/lib/council-context.tsx` (provider + `useCouncil()` hook).
- [ ] Wrap app in provider; replace `tabCouncilFilters` + `getActiveCouncil()` usage with context.
- [ ] Remove `home` from `PageType`, `Sidebar.tsx`, and `renderPage()`; default to `dashboard`.
- [ ] Delete/retire `SuperadminCouncilSelect.tsx` (or repurpose as the dropdown's content).
- [ ] Update `Map.tsx` to read council from context; remove its private `<select>`.
- [ ] Persist selection to `sessionStorage`; restore on load.

**Backend tasks (Track B):**
- [ ] (Optional) Add `GET /api/councils` returning active councils so the hardcoded `COUNCILS`
      array can be removed from the web. (Reference data; `permitAll` acceptable or JWT.)

**Acceptance:** Superadmin sees one council dropdown affecting all pages incl. Map; council-admin
sees a locked council; no "Home" item; selection persists across navigation + reload.

**Notes:**

---

### F2 — Remove "Bin Collection" page; relocate collector-labour (WEB) — **DECIDED: remove**

**Goal:** Remove the Bin Collection page (`schedule` → `CollectionSchedule.tsx`) and relocate its
one real feature (collector-labour management).

**Audit result (evidence for the decision):**
- **Mock / placeholder:** the schedules list, "Today/This Week/Active Crews" stat cards, and the
  "Add Schedule" button (no handler) are all hardcoded — *not meaningful*.
- **Real functionality on the page:**
  - Collector **labour** CRUD → `GET/POST/DELETE /api/collector-labours` (genuinely used).
  - Citizen **event suggestions** approve/reject → duplicated with Citizen Management page.
  - Fetches `/api/users` + `/api/vehicles` but never renders them (dead code).

**Decision:** **Remove the page.** Relocate the two real pieces:
- Collector-labour management → into **Vehicle Management** (route crew) or the Map route-builder.
- Event suggestions → already on the External Users page (F6, citizen sub-tab).

**Web tasks (Track W):**
- [ ] Move collector-labour UI (`GET/POST/DELETE /api/collector-labours`) to Vehicle Management
      (or Map route builder).
- [ ] Remove `schedule` PageType, sidebar item, and `CollectionSchedule.tsx`.
- [ ] Drop the dead `/api/users` + `/api/vehicles` fetches that were only used here.

**Acceptance:** No dead/mock page; the one real feature (labour) survives in a sensible place.

**Notes:**

---

### F3 — Click-to-filter summary cards: Bin Management + Vehicle Management (WEB)

**Goal:** Clicking a top summary card filters the list below to that status.

**Current state:** Cards are display-only. Bin Management cards also count the **wrong**
vocabulary (`critical/warning/normal`) while bins use `full/half/empty/not_checked` → counts read 0.

**Web tasks (Track W) — `BinManagement.tsx`:**
- [ ] Fix status vocabulary so card counts match real bin statuses (`full/half/empty/not_checked`),
      or map fill levels → severity consistently. (Pick one vocabulary and use everywhere.)
- [ ] Add `activeStatusFilter` state; clicking a card toggles filter; active card gets a selected
      style; clicking again clears. Combine with existing search box.
- [ ] Add an "All / Total" card that clears the filter.

**Web tasks (Track W) — `VehicleManagement.tsx`:**
- [ ] Same pattern for `available / on_route / maintenance` + Total.

**Acceptance:** Clicking any card filters the grid; selected state visible; clearing works;
counts are accurate.

**Notes:**

---

### F4 — Map: route display UX (latest by default + toggle to show all) (WEB)

**Goal:** Stop drawing every active route at once (overlap → unreadable). Default to showing the
**most recently created** route; provide a control to toggle individual routes / show-all.

**Current state:** `Map.tsx` `loadActiveSession()` visualizes **all** non-completed sessions on
mount via `visualizeRoutes()`/`visualizeRoutesInternal()` into one `routeLayerRef`.

**Web tasks (Track W) — `Map.tsx`:**
- [ ] On load, fetch active sessions but **render only the latest** (by createdAt) by default.
- [ ] Add a **routes panel/legend** listing each active route (vehicle + color swatch) with a
      per-route visibility toggle (checkbox/switch) and a "Show all / Hide all" master toggle.
- [ ] Maintain a `Map<sessionId, layer>` so toggles add/remove layers without full redraw; assign
      a distinct color per route for clarity.
- [ ] Keep the existing Route History panel; ensure it cooperates with the new visibility model.

**Acceptance:** On open, only one (latest) route is drawn; user can toggle each route on/off and
show-all; no overlapping clutter by default.

**Notes:**

---

### F5 — Automatic zone assignment via clustering (remove manual zone input) (BE/AI + WEB)

**Goal:** Admin no longer types a `zone` when adding a bin. Zones are assigned automatically in
the background by clustering bins per council (K-means or better), feeding route optimization.

**Current state:** Adding a bin **requires** a numeric `zone` (Map.tsx dialog + Bin Management).
`Bin.zone` is free-text; no clustering exists. AI engine repo is empty.

**Approach (DECIDED: backend Java + capacity-aware/balanced clustering):** Implement clustering in
**backend Java** (one deploy unit; OR-Tools already in Java). Use **balanced/capacity-aware K-means
on bin coordinates per council** so clusters are size/capacity-balanced (better for the
capacity-constrained CVRP route optimizer). Choose `k` by heuristic
(`k = ceil(binCount / targetBinsPerZone)`); re-cluster on bin add/remove (debounced).

**Backend tasks (Track B / AI):**
- [ ] Add `ZoneClusteringService` (`core/service/...`) implementing **balanced K-means**
      (e.g. K-means then capacity/size rebalancing pass) over `Bin` coordinates filtered by
      council. Decide `k` heuristic + `targetBinsPerZone`; persist resulting `zone` onto each `Bin`.
- [ ] Hook clustering into bin create/delete (reuse the existing `BinChangedEvent` listener
      pattern that `RouteSessionService.onBinChanged` already uses; debounce).
- [ ] Make `zone` **optional** in `POST /api/bins` (server assigns it). Keep `PUT /bins/{id}/zone`
      for manual override.
- [ ] (Optional) `POST /api/admin/zones/recluster?council=` admin endpoint for manual re-run.
- [ ] (Optional) Expose zone polygons/centroids for map visualization
      (`GET /api/zones?council=`).

**Web tasks (Track W):**
- [ ] Remove the required **zone** input from the Add-Bin dialog in `Map.tsx` and from
      `BinManagement.tsx` create form.
- [ ] (Optional) Visualize zones on the map (color bins by zone / draw cluster hulls) using the
      new zones endpoint.

**App tasks:** none (field staff don't set zones).

**Acceptance:** Admin adds a bin with only location/type; backend assigns a balanced zone; route
optimization still works; manual override still possible.

**Notes:** Clustering = backend Java, balanced/capacity-aware (decided).

---

### F6 — Merge Citizen Management + 3rd Party Collectors into one "External Users" page (WEB + BE)

**Goal:** One sidebar item **External Users** with two sub-tabs: **Citizens** and **Third-Party
Collectors**. Admin can: review/respond to citizen complaints, create events for citizens,
and approve/reject 3rd-party collector registrations including viewing their documents (NIC photo, etc.).

**Current state:**
- `CitizenManagement.tsx`: complaints approve/reject (`PATCH /api/complaints/{id}/status`) +
  event suggestions. No complaint *response text*, no event *creation*.
- `ThirdPartyCollectors.tsx`: lists users where role contains `THIRD`, manual create form, raw
  analytics JSON. **No registration-approval queue, no document viewing.**
- Backend **already** has: complaint status+`resolutionNotes` + assign; event create
  (`POST /api/events`) + approve/reject suggestions; 3rd-party pending list + approve/reject
  (`/api/auth/thirdparty-register/pending|{id}/approve|reject`) with `nicPhotoUrl` stored.

**Web tasks (Track W) — new `ExternalUsers.tsx` with sub-tabs:**
- [ ] **Citizens sub-tab:**
  - [ ] Complaints list with **detail view + response box** → `PATCH /api/complaints/{id}/status`
        sending `status` + `resolutionNotes`. Show complaint image.
  - [ ] **Create event** form → `POST /api/events` (council-scoped). Keep approve/reject of
        citizen event suggestions here too.
- [ ] **Third-Party Collectors sub-tab:**
  - [ ] **Pending registrations queue** → `GET /api/auth/thirdparty-register/pending`.
  - [ ] Detail drawer showing NIC number, **NIC photo (front/back)**, phone, DOB, address,
        company, requested councils.
  - [ ] Approve / Reject buttons → `POST /api/auth/thirdparty-register/{empId}/approve|reject`.
  - [ ] Keep the existing active-collectors list.
- [ ] Remove old `citizen-management` + `third-party-collectors` sidebar items; add `external-users`.

**Backend tasks (Track B):**
- [ ] **Secure** the 3rd-party approval endpoints: move them behind JWT + `ADMIN`/`SUPERADMIN`
      (currently `permitAll`). Either add authenticated `/api/admin/thirdparty/registrations/**`
      endpoints or restrict the existing ones. (Contract in §5.)
- [ ] Ensure complaint detail returns image URL + all fields the UI needs; fix `Complaint` entity
      stub methods if they break the detail/response path.
- [ ] Confirm `nicPhotoUrl` (+ back photo) is returned by the pending/detail endpoint.

**App tasks:** none for the dashboard merge (registration submission already exists — see F7).

**Acceptance:** Single External Users page; admin responds to complaints with notes, creates
events, and approves/rejects collector applications while viewing their documents; approval
endpoints require admin auth.

**Notes:**

---

### F7 — 3rd-party collector application & approval (APP + BE verification)

**Goal:** Verify and finish the end-to-end: collector submits application (with NIC/photo) via app
→ admin reviews & approves/rejects on dashboard (F6) → collector sets password → logs in.

**Current state (verified exists):**
- App: `auth/pages/collector_register.dart` uploads NIC photo
  (`POST /auth/thirdparty-register/nic-photo`) and submits (`POST /auth/thirdparty-register`),
  then polls status (`registration_status.dart`).
- Backend: full registration + approve/reject + `set-password` exist.
- **Gap:** App has **no UI** calling `/{empId}/set-password` — approved users are just told to
  return to login; and approval endpoints are unsecured (fixed in F6).

**App tasks (Track A):**
- [ ] Add **set-password UI** after approval → `POST /auth/thirdparty-register/{empId}/set-password`,
      then route to login. (Wire the existing `setThirdPartyPassword` API method.)
- [ ] Verify the registration flow end-to-end against backend; confirm back-photo upload if required.

**Backend tasks (Track B):**
- [ ] (Shared with F6) secure approve/reject/pending; ensure status polling reflects admin action.
- [ ] Confirm document fields (NIC number, front/back photo URLs) persist and are queryable.

**Acceptance:** A collector can register in-app, be approved in-dashboard (with docs visible), set
a password, and log in. Reject path blocks login with a clear status.

**Notes:**

---

### F8 — Internal user creation + emailed password + first-login change (VERIFY; BE + WEB + APP)

**Goal:** Admin creates internal users (field staff / bin collector) by email; system generates a
password and emails it; user changes it on first login. **This largely exists — verify & polish.**

**Current state (verified exists):**
- Web `InternalUsers.tsx` create form → `POST /api/admins/staff/field-mentors` |
  `/bin-collectors` (no password field — correct; backend generates it).
- Backend `AdminStaffService` generates 12-char temp password, BCrypt-hashes, sets
  `mustChangePassword=true`, emails via `EmailService.sendAdminCredentials`, logs temp password.
- App `change_password.dart` handles forced first-login change (`POST /auth/change-password`).

**Verification tasks:**
- [ ] **BE** Confirm SMTP actually sends (Gmail app password valid); move credentials to env/secret.
- [ ] **BE** Confirm `mustChangePassword` is returned by `/auth/login` and enforced.
- [ ] **WEB + BE** (DECIDED: yes) Allow **superadmin** to create field staff / bin collectors for a
      **chosen council** — add a council picker to the create form (superadmin only) and accept the
      target council server-side in `AdminStaffController`/`AdminStaffService`.
- [ ] **APP** Confirm first-login redirect to change-password and re-login works for both
      field mentor and bin collector roles.
- [ ] **WEB** Add success UX clarifying "password emailed to user".

**Acceptance:** New field-staff/bin-collector receives an email with a temp password, is forced to
change it on first login, then logs in normally. No plaintext passwords exposed in the UI.

**Notes:**

---

### F9 — Real-time dashboard sync: bin reports + collection completion (BE + WEB + APP)

**Goal:** When field staff report a bin or a bin collector completes a collection, the **admin
dashboard updates in real time** (bin status, counts, map).

**Current state:**
- Backend already broadcasts `BIN_STATUS_UPDATED` (raw `/ws-raw`) on field-staff report/undo, and
  `TASK_PROGRESS_UPDATE`/`BIN_COLLECTION_ACK` to the collecting user.
- **The web dashboard does not subscribe to any of this** (only Map subscribes to route-session
  STOMP). Route-session bin `collect` (`PATCH .../collect`) does **not** broadcast.
- App side already consumes `BIN_STATUS_UPDATED` (field staff) and task/route updates.

**Backend tasks (Track B):**
- [ ] Ensure bin-collection completion (route-session `PATCH .../collect` **and**
      `/bincollectors/{id}/collect-bin`) **broadcasts a bin-status/collection event** that the
      dashboard can consume (add a `BIN_COLLECTED`/`BIN_STATUS_UPDATED` broadcast to all admins, or
      a council-scoped topic). Define topic + payload in §5.
- [ ] (Recommended) Add a STOMP topic for admins, e.g. `/topic/councils/{council}/bins`, so the
      web can subscribe with the same STOMP client it already uses in `Map.tsx` (avoids adding a
      raw-WS client to the web).

**Web tasks (Track W):**
- [ ] Add a small shared realtime hook/client (reuse `@stomp/stompjs` already in `package.json`)
      subscribing to the admin bin topic.
- [ ] Live-update **Bin Management** list/cards and **Map** bin markers on bin events (respect the
      selected council from F1).
- [ ] (Optional) Toast/badge for incoming reports.

**App tasks (Track A):**
- [ ] Verify field-staff report + collector completion emit the events the backend expects; fix any
      payload mismatches found during F9 testing.

**Acceptance:** With the dashboard open, a field-staff report or a collector completion updates the
relevant bin in the Bin Management list and on the Map within ~1–2s, scoped to the selected council.

**Notes:**

---

### F10 — Citizen ↔ 3rd-party collector workflow: verify & fix bugs (APP + BE)

**Goal:** Manually verify the marketplace flow and fix bugs: citizen creates request → collectors
see it & send offers → citizen accepts → collector starts/completes → citizen confirms+rates.

**Current state:** Implemented on both app and backend (see `backend-doc/FLOW_CITIZEN_REQUEST.md`,
`FLOW_THIRD_PARTY_COLLECTOR.md`, `COLLECTOR_APPROVAL_MATCHING_*`). App uses 15s polling (no WS).

**Verification tasks (Track A + B):**
- [ ] End-to-end happy path test (citizen → offer → accept → start → complete → confirm+rate).
- [ ] Edge cases: reject offer, withdraw offer, cancel request, cancel accepted job, multiple
      offers (others auto-rejected on accept), expired/closed requests.
- [ ] Verify completion proof (photo + GPS + weight) saved and visible to citizen.
- [ ] Verify council scoping (collector `assignedCouncils` vs citizen council) filters the feed.
- [ ] Log every bug found here as a checkbox sub-item with file + fix.
- [ ] (DECIDED: yes) Replace 15s polling with **WebSocket** real-time for request/offer updates
      on both citizen and 3rd-party sides. Add a STOMP topic (e.g.
      `/topic/users/{userId}/marketplace` or per-request topic) — define in §5; reuse the app's
      existing STOMP client and add a web/app subscription.

**Acceptance:** Full marketplace lifecycle works without manual DB fixes; documented edge cases
behave correctly; bug list closed.

**Bug log:**
- [ ] (add as found)

**Notes:**

---

### F11 — Dashboard UI polish + Dark theme (WEB) — **do last**

**Goal:** Consistent card sections + typography across the dashboard **without changing the existing
(light) color choices**, then add a **dark theme** tuned for the green brand.

**Current state:** Inline Tailwind greens (`green-600/700/50`), inconsistent card styling, dark
tokens scaffolded in `globals.css`/`tailwind.config.ts` but inactive.

**Web tasks (Track W):**
- [ ] Define a small set of reusable card/section components + a typographic scale; refactor pages
      to use them (Dashboard, Bin/Vehicle mgmt, External Users) — **keep current colors**.
- [ ] Tokenize the green brand (CSS vars) so light values are unchanged but dark has a parallel set.
- [ ] Activate dark mode: add a theme toggle (persisted), apply `class="dark"` on `<html>`, fill in
      `.dark` tokens with a green-friendly dark palette (dark surfaces + green accents with adequate
      contrast for charts/Leaflet/status colors).
- [ ] QA contrast (status colors, charts, map legend) in both themes.

**Acceptance:** Visually consistent cards/typography; light theme colors unchanged; a working,
accessible dark theme matching the green brand; toggle persists.

**Notes:**

---

## 5. API / WebSocket Contract Registry (shared — keep authoritative)

> Add every NEW or CHANGED endpoint / WS message here as you implement it, so both devs build
> against the same contract. (Pre-fill before coding.)

### New REST endpoints (proposed)
| Feature | Method | Path | Auth | Request → Response |
|---|---|---|---|---|
| F1 | GET | `/api/councils` | any/admin | → `[{id,name,district,isActive}]` |
| F5 | POST | `/api/admin/zones/recluster?council=` | ADMIN/SUPERADMIN | → cluster summary |
| F5 | GET | `/api/zones?council=` | ADMIN/SUPERADMIN | → zone centroids/polygons |
| F6/F7 | GET | `/api/admin/thirdparty/registrations/pending` | ADMIN/SUPERADMIN | → pending apps + doc URLs |
| F6/F7 | POST | `/api/admin/thirdparty/registrations/{empId}/approve` | ADMIN/SUPERADMIN | → status |
| F6/F7 | POST | `/api/admin/thirdparty/registrations/{empId}/reject` | ADMIN/SUPERADMIN | → status |

### Changed endpoints
| Feature | Endpoint | Change |
|---|---|---|
| F5 | `POST /api/bins` | `zone` becomes **optional** (server assigns) |
| F6 | `GET /api/complaints/{id}` | ensure image URL + all fields returned |

### WebSocket message types / topics
| Feature | Topic | Type | Payload | Direction |
|---|---|---|---|---|
| existing | `/topic/route-sessions/{id}` | route snapshot | `RouteSessionSnapshotDTO` | server→client |
| existing | raw `/ws-raw` | `BIN_STATUS_UPDATED` | bin status | server→all |
| F9 | `/topic/councils/{council}/bins` (proposed) | `BIN_STATUS_UPDATED` / `BIN_COLLECTED` | `{binId,status,fillLevel,council,...}` | server→admins |
| F10 | `/topic/users/{userId}/marketplace` (proposed) | `REQUEST_UPDATED` / `OFFER_UPDATED` | `{requestId,offerId,status,...}` | server→citizen & collector |

---

## 6. Decisions (resolved 2026-06-08)

1. **F2 — Bin Collection page:** ✅ **Remove** it; relocate collector-labour management to
   Vehicle Management / route builder.
2. **F1 — Council UX:** ✅ **Single top-bar dropdown** with "All Councils" (superadmin only).
3. **F5 — Clustering home & algorithm:** ✅ **Backend Java**, **balanced / capacity-aware**
   clustering. (Still to set: `targetBinsPerZone` value — default `~15` unless told otherwise.)
4. **F8 — Superadmin create internal users:** ✅ **Yes** — superadmin can create for a chosen
   council (add council picker).
5. **F10 — Realtime for marketplace:** ✅ **WebSocket now** (replace 15s polling).
6. **F11 — Dark theme:** ⏳ Still need preferred dark surface shade — default assumption: slate /
   near-black base with green accents (change here if undesired).

---

## 7. Suggested execution timeline (editable)

| Sprint | Web (Track W) | App (Track A) | Backend (Track B) |
|---|---|---|---|
| 1 | F1 nav/council, F3 cards | F8 verify, F7 set-password UI | F1 `/councils`, F9 admin topic contract |
| 2 | F4 map routes, F9 web subscribe | F10 verify+bugs | F9 broadcasts, F6 secure 3rd-party |
| 3 | F6 External Users page | F10 fixes, F7 e2e | F5 clustering service |
| 4 | F5 web (remove zone), F2 (after approval) | F10 optional WS | F5 hooks, fixes |
| 5 | F11 UI polish + dark theme | regression test | hardening (security, stubs) |

---

## 8. Definition of done (whole effort)
- [ ] No "Home" page; one global council control (superadmin) applied everywhere.
- [ ] Bin Collection page resolved per F2 decision.
- [ ] Card click-filtering on Bin + Vehicle management with correct counts.
- [ ] Map shows latest route by default with per-route toggles + show-all.
- [ ] Zones auto-assigned (no manual zone input); routing still works.
- [ ] Single External Users page: complaint responses, event creation, collector approval + docs.
- [ ] 3rd-party register→approve→set-password→login works end-to-end.
- [ ] Internal-user email + first-login change verified.
- [ ] Dashboard reflects bin reports + collections in real time.
- [ ] Citizen↔collector marketplace verified, bugs fixed.
- [ ] Consistent UI + working green-friendly dark theme (colors otherwise unchanged).
- [ ] Cross-cutting backend issues (stubs, security, secrets) addressed.
