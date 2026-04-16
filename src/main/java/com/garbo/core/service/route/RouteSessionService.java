package com.garbo.core.service.route;

import com.garbo.api.dto.*;
import com.garbo.api.dto.RouteResponseDTO.BinStop;
import com.garbo.api.dto.RouteResponseDTO.VehicleRoute;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import com.garbo.domain.OSRMClient;
import com.garbo.domain.ORToolsWrapper;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RouteSessionService {

    private static final long RECOMPUTE_DEBOUNCE_MS = 1500;

    private final BinRepository binRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, RouteSessionState> sessionsById = new ConcurrentHashMap<>();
    private final Map<Long, String> activeSessionIdByUser = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService computePool = Executors.newFixedThreadPool(2);

    public RouteSessionService(BinRepository binRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.binRepository = binRepository;
        this.messagingTemplate = messagingTemplate;
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

            return ready;

        } catch (Exception e) {
            return error(state, e.getMessage());
        }
    }

    // =========================================================
    // BIN LOADING (IMPORTANT FIX)
    // =========================================================
    private List<Bin> loadBins(RouteSessionCreateRequestDTO config) {

        List<Long> selected = config.getSelectedBinIds();

        if (selected == null || selected.isEmpty()) {
            return binRepository.findAll();
        }

        List<Bin> bins = binRepository.findAllById(selected);

        Map<Long, Bin> map = new HashMap<>();
        for (Bin b : bins) {
            map.put(b.getId(), b);
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
        Map<Integer, List<Long>> routes =
                solver.solve(matrix, bins, vehicleCount, capacities);

        Map<Long, Bin> lookup = new HashMap<>();
        for (Bin b : bins) lookup.put(b.getId(), b);

        Map<Integer, VehicleRoute> result = new LinkedHashMap<>();

        for (int v = 0; v < vehicleCount; v++) {

            List<Long> routeBins = routes.getOrDefault(v, Collections.emptyList());
            if (routeBins.isEmpty()) continue;

            List<BinStop> stops = new ArrayList<>();
            double total = 0;

            for (int i = 0; i < routeBins.size(); i++) {
                Bin b = lookup.get(routeBins.get(i));

                double dist = 0; // simplified (can improve later)
                total += dist;

                stops.add(new BinStop(
                        i + 1,
                        b.getId(),
                        b.getLat(),
                        b.getLng(),
                        dist
                ));
            }

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

        CompletableFuture.runAsync(() -> {

            List<Bin> bins = loadBins(state.getConfig());
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

        }, computePool);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private List<Long> extractIds(List<Bin> bins) {
        List<Long> ids = new ArrayList<>();
        for (Bin b : bins) ids.add(b.getId());
        return ids;
    }

    private void publish(RouteSessionSnapshotDTO snapshot) {
        messagingTemplate.convertAndSend("/topic/routes/users/" + snapshot.userId, snapshot);
        messagingTemplate.convertAndSend("/topic/route-sessions/" + snapshot.sessionId, snapshot);
    }

    private RouteSessionSnapshotDTO error(RouteSessionState state, String msg) {
        return RouteSessionSnapshotDTO.error(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "ERROR",
                state.getConfig().getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList(),
                msg
        );
    }

    private void validateRequest(RouteSessionCreateRequestDTO request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("Invalid request");
        }
    }

    private RouteSessionState registerSession(RouteSessionCreateRequestDTO request) {

        Long userId = request.getUserId();
        String sessionId = UUID.randomUUID().toString();

        RouteSessionState state = new RouteSessionState(sessionId, userId, request);

        synchronized (this) {
            String old = activeSessionIdByUser.put(userId, sessionId);
            if (old != null) sessionsById.remove(old);

            sessionsById.put(sessionId, state);
        }

        return state;
    }

    // ============================
    // SIMPLE ACCESSORS
    // ============================
    public RouteSessionSnapshotDTO getLatestSnapshot(String sessionId) {
        return sessionsById.get(sessionId).getLatest();
    }

    public RouteSessionSnapshotDTO getLatestSnapshotByUser(Long userId) {
        String sid = activeSessionIdByUser.get(userId);
        return sid == null ? null : getLatestSnapshot(sid);
    }

    public RouteSessionSnapshotDTO recompute(String sessionId) {
        scheduleRecompute(sessionId, "MANUAL", 0);
        return getLatestSnapshot(sessionId);
    }

    public RouteSessionSnapshotDTO recomputeByUser(Long userId) {
        String sid = activeSessionIdByUser.get(userId);
        if (sid == null) return null;
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
















/*package com.garbo.core.service.route;

import com.garbo.api.dto.RouteResponseDTO;
import com.garbo.api.dto.RouteResponseDTO.BinStop;
import com.garbo.api.dto.RouteResponseDTO.VehicleRoute;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionCreateResponseDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import com.garbo.domain.ORToolsWrapper;
import com.garbo.domain.OSRMClient;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RouteSessionService {

    private static final long RECOMPUTE_DEBOUNCE_MS = 1500;

    private final BinRepository binRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentMap<String, RouteSessionState> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> activeSessionIdByUser = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService computePool = Executors.newFixedThreadPool(2);

    public RouteSessionService(BinRepository binRepository, SimpMessagingTemplate messagingTemplate) {
        this.binRepository = binRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public RouteSessionCreateResponseDTO createOrReplaceSession(RouteSessionCreateRequestDTO request) {
        validateRequest(request);

        RouteSessionState state = registerSession(request);

        RouteSessionSnapshotDTO processing = RouteSessionSnapshotDTO.processing(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "SESSION_CREATED",
                state.getConfig().getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        state.setLatest(processing);
        publishSnapshot(processing);

        scheduleRecompute(state.getSessionId(), "SESSION_CREATED", 0);

        return new RouteSessionCreateResponseDTO(
                state.getSessionId(),
                state.getUserId(),
                buildUserTopic(state.getUserId()),
                state.getLatest()
        );
    }

    public RouteSessionSnapshotDTO optimizeAndBroadcast(RouteSessionCreateRequestDTO request) {
        validateRequest(request);

        RouteSessionState state = registerSession(request);

        RouteSessionSnapshotDTO processing = RouteSessionSnapshotDTO.processing(
                state.getSessionId(),
                state.getUserId(),
                state.getVersion().incrementAndGet(),
                "HTTP_OPTIMIZE",
                state.getConfig().getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        state.setLatest(processing);
        publishSnapshot(processing);

        try {
            List<Bin> bins = loadSessionBins(state.getConfig());
            if (bins.isEmpty()) {
                return publishOptimizationError(state, "No bins found for this session.");
            }

            List<Long> currentBinIds = new ArrayList<>();
            for (Bin bin : bins) {
                currentBinIds.add(bin.getId());
            }

            RouteResponseDTO route = buildOptimizedRoute(state.getConfig(), bins);
            long version = state.getVersion().incrementAndGet();
            RouteSessionSnapshotDTO ready = RouteSessionSnapshotDTO.ready(
                    state.getSessionId(),
                    state.getUserId(),
                    version,
                    "HTTP_OPTIMIZE",
                    state.getConfig().getSelectedBinIds(),
                    currentBinIds,
                    Collections.emptyList(),
                    route
            );

            state.setActiveBinIds(currentBinIds);
            state.setLatest(ready);
            publishSnapshot(ready);
            return ready;
        } catch (Exception ex) {
            return publishOptimizationError(state, "Error optimizing routes: " + ex.getMessage());
        }
    }

    public RouteSessionSnapshotDTO getLatestSnapshot(String sessionId) {
        return getSessionOrThrow(sessionId).getLatest();
    }

    public RouteSessionSnapshotDTO getLatestSnapshotByUser(Long userId) {
        return getSessionByUserOrThrow(userId).getLatest();
    }

    public RouteSessionSnapshotDTO triggerRecompute(String sessionId) {
        getSessionOrThrow(sessionId);
        scheduleRecompute(sessionId, "MANUAL", 0);
        return getLatestSnapshot(sessionId);
    }

    public RouteSessionSnapshotDTO triggerRecomputeByUser(Long userId) {
        RouteSessionState state = getSessionByUserOrThrow(userId);
        scheduleRecompute(state.getSessionId(), "MANUAL", 0);
        return state.getLatest();
    }

    public void deleteSession(String sessionId) {
        RouteSessionState removed = cancelAndRemove(sessionId);
        if (removed == null) {
            return;
        }

        activeSessionIdByUser.remove(removed.getUserId(), sessionId);
    }

    public void deleteSessionByUser(Long userId) {
        String sessionId = activeSessionIdByUser.remove(userId);
        if (sessionId == null) {
            return;
        }
        cancelAndRemove(sessionId);
    }

    @EventListener
    public void onBinChanged(BinChangedEvent event) {
        for (RouteSessionState state : sessionsById.values()) {
            if (!isRelevant(state, event.getBinId())) {
                continue;
            }
            scheduleRecompute(state.getSessionId(), "BIN_" + event.getChangeType(), RECOMPUTE_DEBOUNCE_MS);
        }
    }

    private boolean isRelevant(RouteSessionState state, Long changedBinId) {
        List<Long> selected = state.getConfig().getSelectedBinIds();
        if (selected == null || selected.isEmpty()) {
            return true;
        }
        return changedBinId != null && selected.contains(changedBinId);
    }

    private void scheduleRecompute(String sessionId, String trigger, long delayMs) {
        RouteSessionState state = sessionsById.get(sessionId);
        if (state == null) {
            return;
        }

        synchronized (state) {
            if (state.getScheduledFuture() != null) {
                state.getScheduledFuture().cancel(false);
            }

            long generation = state.nextGeneration();
                long version = state.getVersion().incrementAndGet();

            RouteSessionSnapshotDTO processing = RouteSessionSnapshotDTO.processing(
                    state.getSessionId(),
                    state.getUserId(),
                    version,
                    trigger,
                    state.getConfig().getSelectedBinIds(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            state.setLatest(processing);
            publishSnapshot(processing);

            state.setScheduledFuture(scheduler.schedule(
                    () -> startCompute(sessionId, generation, trigger),
                    delayMs,
                    TimeUnit.MILLISECONDS
            ));
        }
    }

    private void startCompute(String sessionId, long generation, String trigger) {
        RouteSessionState state = sessionsById.get(sessionId);
        if (state == null || generation != state.getGeneration()) {
            return;
        }

        CompletableFuture<Void> task = CompletableFuture.runAsync(
                () -> computeAndPublishSnapshot(state, generation, trigger),
                computePool
        );
        state.setRunningFuture(task);
    }

    private void computeAndPublishSnapshot(RouteSessionState state, long generation, String trigger) {
        try {
            List<Bin> bins = loadSessionBins(state.getConfig());
            List<Long> currentBinIds = new ArrayList<>();
            for (Bin bin : bins) {
                currentBinIds.add(bin.getId());
            }

            List<Long> previousBinIds = new ArrayList<>(state.getActiveBinIds());
            List<Long> addedBinIds = diff(currentBinIds, previousBinIds);
            List<Long> removedBinIds = diff(previousBinIds, currentBinIds);

            if (bins.isEmpty()) {
                publishErrorIfLatest(state, generation, trigger, addedBinIds, removedBinIds,
                        "No bins found for this session.");
                return;
            }

            RouteResponseDTO route = buildOptimizedRoute(state.getConfig(), bins);
            publishReadyIfLatest(state, generation, trigger, currentBinIds, addedBinIds, removedBinIds, route);
        } catch (Exception ex) {
            publishErrorIfLatest(state, generation, trigger, Collections.emptyList(), Collections.emptyList(),
                    "Error optimizing routes: " + ex.getMessage());
        }
    }

    private List<Long> diff(List<Long> left, List<Long> right) {
        List<Long> result = new ArrayList<>();
        for (Long value : left) {
            if (!right.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Bin> loadSessionBins(RouteSessionCreateRequestDTO config) {
        List<Long> selected = config.getSelectedBinIds();

        if (selected == null || selected.isEmpty()) {
            return binRepository.findAll();
        }

        List<Bin> bins = binRepository.findAllById(selected);
        Map<Long, Bin> byId = new HashMap<>();
        for (Bin bin : bins) {
            byId.put(bin.getId(), bin);
        }

        List<Bin> ordered = new ArrayList<>();
        for (Long id : selected) {
            Bin bin = byId.get(id);
            if (bin != null) {
                ordered.add(bin);
            }
        }

        return ordered;
    }

    private RouteResponseDTO buildOptimizedRoute(RouteSessionCreateRequestDTO request, List<Bin> bins) {
        int totalNodes = bins.size() + 1;
        double[][] coords = new double[totalNodes][2];
        coords[0][0] = request.getDepotLat();
        coords[0][1] = request.getDepotLng();

        for (int i = 0; i < bins.size(); i++) {
            coords[i + 1][0] = bins.get(i).getLat();
            coords[i + 1][1] = bins.get(i).getLng();
        }

        double[][] durationMatrix = OSRMClient.getDurationMatrix(coords);

        int vehicleCount = request.getVehicleCount() > 0 ? request.getVehicleCount() : 1;
        int[] capacities = request.getValidatedCapacities();

        ORToolsWrapper wrapper = new ORToolsWrapper();
        Map<Integer, List<Long>> rawRoutes = wrapper.solve(durationMatrix, bins, vehicleCount, capacities);

        Map<Long, Bin> binLookup = new HashMap<>();
        Map<Long, Integer> nodeLookup = new HashMap<>();

        for (int i = 0; i < bins.size(); i++) {
            Bin bin = bins.get(i);
            binLookup.put(bin.getId(), bin);
            nodeLookup.put(bin.getId(), i + 1);
        }

        int vehiclesUsed = 0;
        Map<Integer, VehicleRoute> detailedRoutes = new LinkedHashMap<>();

        for (int vehicleId = 0; vehicleId < vehicleCount; vehicleId++) {
            List<Long> binIds = rawRoutes.getOrDefault(vehicleId, Collections.emptyList());
            if (binIds.isEmpty()) {
                continue;
            }

            vehiclesUsed++;
            List<BinStop> sequence = new ArrayList<>();
            double totalDuration = 0.0;

            for (int stop = 0; stop < binIds.size(); stop++) {
                long binId = binIds.get(stop);
                Bin bin = binLookup.get(binId);
                int currentNode = nodeLookup.get(binId);

                double fromPrev = stop == 0
                        ? durationMatrix[0][currentNode]
                        : durationMatrix[nodeLookup.get(binIds.get(stop - 1))][currentNode];

                totalDuration += fromPrev;

                sequence.add(new BinStop(
                        stop + 1,
                        binId,
                        bin.getLat(),
                        bin.getLng(),
                        fromPrev
                ));
            }

            int lastNode = nodeLookup.get(binIds.get(binIds.size() - 1));
            totalDuration += durationMatrix[lastNode][0];

            detailedRoutes.put(vehicleId, new VehicleRoute(
                    vehicleId,
                    capacities[vehicleId],
                    totalDuration,
                    sequence
            ));
        }

        return new RouteResponseDTO(vehiclesUsed, detailedRoutes);
    }

    private void publishReadyIfLatest(
            RouteSessionState state,
            long generation,
            String trigger,
            List<Long> currentBinIds,
            List<Long> addedBinIds,
            List<Long> removedBinIds,
            RouteResponseDTO route
    ) {
        if (!isCurrentGeneration(state, generation)) {
            return;
        }

        long version = state.getVersion().incrementAndGet();
        RouteSessionSnapshotDTO ready = RouteSessionSnapshotDTO.ready(
                state.getSessionId(),
                state.getUserId(),
                version,
                trigger,
                state.getConfig().getSelectedBinIds(),
                addedBinIds,
                removedBinIds,
                route
        );

        state.setActiveBinIds(currentBinIds);
        state.setLatest(ready);
        publishSnapshot(ready);
    }

    private void publishErrorIfLatest(
            RouteSessionState state,
            long generation,
            String trigger,
            List<Long> addedBinIds,
            List<Long> removedBinIds,
            String message
    ) {
        if (!isCurrentGeneration(state, generation)) {
            return;
        }

        long version = state.getVersion().incrementAndGet();
        RouteSessionSnapshotDTO error = RouteSessionSnapshotDTO.error(
                state.getSessionId(),
                state.getUserId(),
                version,
                trigger,
                state.getConfig().getSelectedBinIds(),
                addedBinIds,
                removedBinIds,
                message
        );

        state.setLatest(error);
        publishSnapshot(error);
    }

    private boolean isCurrentGeneration(RouteSessionState state, long generation) {
        RouteSessionState current = sessionsById.get(state.getSessionId());
        return current != null && current.getGeneration() == generation;
    }

    private RouteSessionState cancelAndRemove(String sessionId) {
        RouteSessionState removed = sessionsById.remove(sessionId);
        if (removed == null) {
            return null;
        }

        if (removed.getScheduledFuture() != null) {
            removed.getScheduledFuture().cancel(false);
        }
        if (removed.getRunningFuture() != null) {
            removed.getRunningFuture().cancel(true);
        }

        return removed;
    }

    private RouteSessionState registerSession(RouteSessionCreateRequestDTO request) {
        Long userId = request.getUserId();
        String sessionId = UUID.randomUUID().toString();
        RouteSessionState state = new RouteSessionState(sessionId, userId, request);

        synchronized (this) {
            String existingSessionId = activeSessionIdByUser.put(userId, sessionId);
            if (existingSessionId != null) {
                cancelAndRemove(existingSessionId);
            }
            sessionsById.put(sessionId, state);
        }

        return state;
    }

    private RouteSessionSnapshotDTO publishOptimizationError(RouteSessionState state, String message) {
        long version = state.getVersion().incrementAndGet();
        RouteSessionSnapshotDTO error = RouteSessionSnapshotDTO.error(
                state.getSessionId(),
                state.getUserId(),
                version,
                "HTTP_OPTIMIZE",
                state.getConfig().getSelectedBinIds(),
                Collections.emptyList(),
                Collections.emptyList(),
                message
        );
        state.setLatest(error);
        publishSnapshot(error);
        return error;
    }

    private void publishSnapshot(RouteSessionSnapshotDTO snapshot) {
        messagingTemplate.convertAndSend(buildUserTopic(snapshot.userId), snapshot);
        messagingTemplate.convertAndSend("/topic/route-sessions/" + snapshot.sessionId, snapshot);
    }

    private String buildUserTopic(Long userId) {
        return "/topic/routes/users/" + userId;
    }

    private void validateRequest(RouteSessionCreateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("userId is required.");
        }
        if (!request.hasValidDepot()) {
            throw new IllegalArgumentException("Missing depot location. Please provide depotLat and depotLng.");
        }
    }

    private RouteSessionState getSessionOrThrow(String sessionId) {
        RouteSessionState state = sessionsById.get(sessionId);
        if (state == null) {
            throw new IllegalArgumentException("Route session not found: " + sessionId);
        }
        return state;
    }

    private RouteSessionState getSessionByUserOrThrow(Long userId) {
        String sessionId = activeSessionIdByUser.get(userId);
        if (sessionId == null) {
            throw new IllegalArgumentException("No active route session for userId: " + userId);
        }
        return getSessionOrThrow(sessionId);
    }
}

*/
