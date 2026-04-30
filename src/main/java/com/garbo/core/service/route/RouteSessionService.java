package com.garbo.core.service.route;

import com.garbo.api.dto.*;
import com.garbo.api.dto.RouteResponseDTO.BinStop;
import com.garbo.api.dto.RouteResponseDTO.VehicleRoute;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import com.garbo.domain.OSRMClient;
import com.garbo.domain.ORToolsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class RouteSessionService {

    private static final long RECOMPUTE_DEBOUNCE_MS = 1500;

    private final BinRepository binRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteAssignmentService routeAssignmentService;

    private final Map<String, RouteSessionState> sessionsById = new ConcurrentHashMap<>();
    private final Map<Long, String> activeSessionIdByUser   = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler    = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService          computePool  = Executors.newFixedThreadPool(2);

    public RouteSessionService(BinRepository binRepository,
                               SimpMessagingTemplate messagingTemplate,
                               RouteAssignmentService routeAssignmentService) {
        this.binRepository         = binRepository;
        this.messagingTemplate     = messagingTemplate;
        this.routeAssignmentService = routeAssignmentService;
    }

    // =========================================================
    // SESSION CREATE
    // =========================================================
    public RouteSessionSnapshotDTO createSession(RouteSessionCreateRequestDTO request) {

        validateRequest(request);

        RouteSessionState state = registerSession(request);

        RouteSessionSnapshotDTO snapshot = RouteSessionSnapshotDTO.processing(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "SESSION_CREATED",
                request.getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        state.setLatest(snapshot);
        publish(snapshot);

        scheduleRecompute(state.getSessionId(), "SESSION_CREATED", 0);

        return snapshot;
    }

    // =========================================================
    // MAIN OPTIMIZE ENTRY (HTTP)
    // =========================================================
    public RouteSessionSnapshotDTO optimizeAndBroadcast(RouteSessionCreateRequestDTO request) {

        validateRequest(request);

        RouteSessionState state = registerSession(request);

        RouteSessionSnapshotDTO processing = RouteSessionSnapshotDTO.processing(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "HTTP_OPTIMIZE",
                request.getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        state.setLatest(processing);
        publish(processing);

        try {
            List<Bin> bins = loadBins(state.getConfig());

            if (bins.isEmpty()) {
                return error(state, "No bins found");
            }

            RouteResponseDTO route = solveRoute(state.getConfig(), bins);

            RouteSessionSnapshotDTO ready = RouteSessionSnapshotDTO.ready(
                    state.getSessionId(),
                    state.getUserId(),
                    state.getVersion().incrementAndGet(),
                    "HTTP_OPTIMIZE",
                    request.getSelectedBinIds(),
                    extractIds(bins),
                    Collections.emptyList(),
                    route
            );

            state.setActiveBinIds(extractIds(bins));
            state.setLatest(ready);
            publish(ready);

            // ── Persist to DB if the request carries valid team details ──────
            // RouteSessionController already calls persist() directly after this
            // method returns, so the instanceof guard here only fires when
            // optimizeAndBroadcast() is called with a RouteSessionCreateRequestDTO
            // that also happens to be a RouteAssignmentRequestDTO (direct call path).
            if (request instanceof RouteAssignmentRequestDTO assignmentRequest
                    && assignmentRequest.hasValidTeam()) {
                try {
                    routeAssignmentService.persist(assignmentRequest, ready);
                    log.info("Route persisted inline from optimizeAndBroadcast: sessionId={}",
                            ready.sessionId);
                } catch (Exception e) {
                    // Non-fatal — in-memory session and WebSocket broadcast already succeeded
                    log.warn("Route optimized but inline DB persist failed: {}", e.getMessage());
                }
            }
            // ────────────────────────────────────────────────────────────────

            return ready;

        } catch (Exception e) {
            return error(state, e.getMessage());
        }
    }

    // =========================================================
    // BIN LOADING
    // =========================================================
    private List<Bin> loadBins(RouteSessionCreateRequestDTO config) {

        List<Long> selected = config.getSelectedBinIds();

        if (selected == null || selected.isEmpty()) {
            return binRepository.findAll();
        }

        List<Bin> bins = binRepository.findAllByIdWithCast(selected);

        Map<Long, Bin> map = new HashMap<>();
        for (Bin b : bins) {
            map.put((Long) b.getId(), b);
        }

        List<Bin> ordered = new ArrayList<>();
        for (Long id : selected) {
            if (map.containsKey(id)) {
                ordered.add(map.get(id));
            }
        }

        return ordered;
    }

    // =========================================================
    // ROUTE SOLVER PIPELINE
    // =========================================================
    private RouteResponseDTO solveRoute(RouteSessionCreateRequestDTO request,
                                        List<Bin> bins) {

        int n = bins.size() + 1;

        double[][] coords = new double[n][2];
        coords[0][0] = request.getDepotLat();
        coords[0][1] = request.getDepotLng();

        for (int i = 0; i < bins.size(); i++) {
            coords[i + 1][0] = bins.get(i).getLat();
            coords[i + 1][1] = bins.get(i).getLng();
        }

        double[][] matrix = OSRMClient.getDurationMatrix(coords);

        int vehicleCount = Math.max(1, request.getVehicleCount());
        int[] capacities = request.getValidatedCapacities();

        ORToolsWrapper solver = new ORToolsWrapper();
        Map<Integer, List<Long>> routes = solver.solve(matrix, bins, vehicleCount, capacities);

        Map<Long, Bin>    lookup     = new HashMap<>();
        Map<Long, Integer> nodeLookup = new HashMap<>();
        for (int i = 0; i < bins.size(); i++) {
            Bin b = bins.get(i);
            lookup.put((Long) b.getId(), b);
            nodeLookup.put((Long) b.getId(), i + 1);
        }

        Map<Integer, VehicleRoute> result = new LinkedHashMap<>();

        for (int v = 0; v < vehicleCount; v++) {

            List<Long> routeBins = routes.getOrDefault(v, Collections.emptyList());
            if (routeBins.isEmpty()) continue;

            List<BinStop> stops = new ArrayList<>();
            double total = 0;

            for (int i = 0; i < routeBins.size(); i++) {
                Bin b = lookup.get(routeBins.get(i));
                int currentNode = nodeLookup.get((Long) b.getId());

                double fromPrev = i == 0
                        ? matrix[0][currentNode]
                        : matrix[nodeLookup.get(routeBins.get(i - 1))][currentNode];

                total += fromPrev;

                stops.add(new BinStop(i + 1, b.getId(), b.getLat(), b.getLng(), fromPrev));
            }

            // Add return-to-depot duration
            int lastNode = nodeLookup.get(routeBins.get(routeBins.size() - 1));
            total += matrix[lastNode][0];

            result.put(v, new VehicleRoute(v, capacities[v], total, stops));
        }

        return new RouteResponseDTO(result.size(), result);
    }

    // =========================================================
    // EVENT TRIGGER
    // =========================================================
    @EventListener
    public void onBinChanged(BinChangedEvent event) {

        for (RouteSessionState state : sessionsById.values()) {

            List<Long> selected = state.getConfig().getSelectedBinIds();
            if (selected != null && !selected.contains(event.getBinId())) continue;

            scheduleRecompute(state.getSessionId(), "BIN_" + event.getChangeType(), RECOMPUTE_DEBOUNCE_MS);
        }
    }

    // =========================================================
    // RECOMPUTE SYSTEM
    // =========================================================
    private void scheduleRecompute(String sessionId, String trigger, long delay) {

        RouteSessionState state = sessionsById.get(sessionId);
        if (state == null) return;

        synchronized (state) {

            if (state.getScheduledFuture() != null) {
                state.getScheduledFuture().cancel(false);
            }

            long gen = state.nextGeneration();
            long ver = state.getVersion().incrementAndGet();

            RouteSessionSnapshotDTO processing = RouteSessionSnapshotDTO.processing(
                    state.getSessionId(),
                    state.getUserId(),
                    ver,
                    trigger,
                    state.getConfig().getSelectedBinIds(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            state.setLatest(processing);
            publish(processing);

            state.setScheduledFuture(
                    scheduler.schedule(
                            () -> runCompute(state, gen, trigger),
                            delay,
                            TimeUnit.MILLISECONDS
                    )
            );
        }
    }

    private void runCompute(RouteSessionState state, long gen, String trigger) {

        if (gen != state.getGeneration()) return;

        CompletableFuture.runAsync(() -> {

            try {
                List<Bin> bins  = loadBins(state.getConfig());
                RouteResponseDTO route = solveRoute(state.getConfig(), bins);

                RouteSessionSnapshotDTO ready = RouteSessionSnapshotDTO.ready(
                        state.getSessionId(),
                        state.getUserId(),
                        state.getVersion().incrementAndGet(),
                        trigger,
                        state.getConfig().getSelectedBinIds(),
                        extractIds(bins),
                        Collections.emptyList(),
                        route
                );

                state.setLatest(ready);
                publish(ready);

                // Persist recomputed route if the session config has team details
                RouteSessionCreateRequestDTO config = state.getConfig();
                if (config instanceof RouteAssignmentRequestDTO assignmentRequest
                        && assignmentRequest.hasValidTeam()) {
                    try {
                        routeAssignmentService.persist(assignmentRequest, ready);
                        log.info("Recomputed route persisted: sessionId={}", ready.sessionId);
                    } catch (Exception e) {
                        log.warn("Recomputed route persist failed: {}", e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Error during background recompute for session {}: {}",
                        state.getSessionId(), e.getMessage());
            }

        }, computePool);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private List<Long> extractIds(List<Bin> bins) {
        List<Long> ids = new ArrayList<>();
        for (Bin b : bins) ids.add((Long) b.getId());
        return ids;
    }

    private void publish(RouteSessionSnapshotDTO snapshot) {
        messagingTemplate.convertAndSend("/topic/routes/users/" + snapshot.userId, snapshot);
        messagingTemplate.convertAndSend("/topic/route-sessions/" + snapshot.sessionId, snapshot);
    }

    private RouteSessionSnapshotDTO error(RouteSessionState state, String msg) {
        RouteSessionSnapshotDTO err = RouteSessionSnapshotDTO.error(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "ERROR",
                state.getConfig().getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList(),
                msg
        );
        state.setLatest(err);
        publish(err);
        return err;
    }

    private void validateRequest(RouteSessionCreateRequestDTO request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("Invalid request: userId is required");
        }
    }

    private RouteSessionState registerSession(RouteSessionCreateRequestDTO request) {

        Long   userId    = request.getUserId();
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        RouteSessionState state = new RouteSessionState(sessionId, userId, request);

        synchronized (this) {
            String old = activeSessionIdByUser.put(userId, sessionId);
            if (old != null) sessionsById.remove(old);
            sessionsById.put(sessionId, state);
        }

        return state;
    }

    // =========================================================
    // PUBLIC ACCESSORS
    // =========================================================
    public RouteSessionSnapshotDTO getLatestSnapshot(String sessionId) {
        RouteSessionState state = sessionsById.get(sessionId);
        if (state == null) throw new IllegalArgumentException("Session not found: " + sessionId);
        return state.getLatest();
    }

    public RouteSessionSnapshotDTO getLatestSnapshotByUser(Long userId) {
        String sid = activeSessionIdByUser.get(userId);
        if (sid == null) throw new IllegalArgumentException("No active session for user: " + userId);
        return getLatestSnapshot(sid);
    }

    public RouteSessionSnapshotDTO recompute(String sessionId) {
        if (!sessionsById.containsKey(sessionId)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        scheduleRecompute(sessionId, "MANUAL", 0);
        return getLatestSnapshot(sessionId);
    }

    public RouteSessionSnapshotDTO recomputeByUser(Long userId) {
        String sid = activeSessionIdByUser.get(userId);
        if (sid == null) throw new IllegalArgumentException("No active session for user: " + userId);
        return recompute(sid);
    }

    public void deleteSession(String sessionId) {
        sessionsById.remove(sessionId);
    }

    public void deleteByUser(Long userId) {
        String sid = activeSessionIdByUser.remove(userId);
        if (sid != null) sessionsById.remove(sid);
    }
}