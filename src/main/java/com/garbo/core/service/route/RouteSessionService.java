package com.garbo.core.service.route;

import com.garbo.api.dto.RouteResponseDTO;
import com.garbo.api.dto.RouteResponseDTO.BinStop;
import com.garbo.api.dto.RouteResponseDTO.VehicleRoute;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.api.dto.websocket.RouteUpdatePayload;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.service.event.BinChangedEvent;
import com.garbo.infrastructure.websocket.RouteBroadcaster;
import com.garbo.domain.ORToolsWrapper;
import com.garbo.domain.OSRMClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouteSessionService {

    private static final long RECOMPUTE_DEBOUNCE_MS = 1500;

    private final BinRepository binRepository;
    private final BinCollectorRepository binCollectorRepository;
    private final RouteBroadcaster routeBroadcaster;

    private final ConcurrentMap<String, RouteSessionState> sessionsById = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService computePool = Executors.newFixedThreadPool(2);

    public RouteSessionService(BinRepository binRepository, BinCollectorRepository binCollectorRepository, RouteBroadcaster routeBroadcaster) {
        this.binRepository = binRepository;
        this.binCollectorRepository = binCollectorRepository;
        this.routeBroadcaster = routeBroadcaster;
    }

    public RouteSessionSnapshotDTO optimizeAndBroadcast(RouteSessionCreateRequestDTO request) {
        validateRequest(request);

        String workShift = resolveWorkShift(request.getUserId());
        RouteSessionSnapshotDTO conflict = validateShiftBinAvailability(request, workShift, request.getSessionId(), "HTTP_OPTIMIZE");
        if (conflict != null) {
            log.warn("Route optimize rejected due to duplicate bin assignment in shift: userId={}, workShift={}, message={}", request.getUserId(), workShift, conflict.message);
            return conflict;
        }

        RouteSessionState state = upsertSession(request, workShift);
        log.info(
            "Route optimize request accepted: requestedSessionId={}, sessionId={}, userId={}, selectedBins={}, vehicleCount={}",
            request.getSessionId(),
            state.getSessionId(),
            state.getUserId(),
            state.getConfig().getSelectedBinIds(),
            state.getConfig().getVehicleCount()
        );

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


    @PreDestroy
    public void shutdownExecutors() {
        scheduler.shutdownNow();
        computePool.shutdownNow();
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

        List<Bin> bins;
        try {
            bins = binRepository.findAllById(selected);
        } catch (Exception ignored) {
            bins = binRepository.findAllByTextIdsCastToBigInt(selected);
        }
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

    private RouteSessionState upsertSession(RouteSessionCreateRequestDTO request, String workShift) {
        synchronized (this) {
            String requestedSessionId = request.getSessionId();
            if (requestedSessionId != null && !requestedSessionId.isBlank()) {
                RouteSessionState existing = sessionsById.get(requestedSessionId);
                if (existing != null) {
                    if (!existing.getUserId().equals(request.getUserId())) {
                        throw new IllegalArgumentException("Session does not belong to this user.");
                    }
                    existing.setConfig(request);
                    existing.setWorkShift(workShift);
                    return existing;
                }
            }

            String sessionId = requestedSessionId;
            if (sessionId == null || sessionId.isBlank() || sessionsById.containsKey(sessionId)) {
                sessionId = UUID.randomUUID().toString();
            }

            RouteSessionState state = new RouteSessionState(sessionId, request.getUserId(), request);
            state.setWorkShift(workShift);
            sessionsById.put(sessionId, state);
            return state;
        }
    }

    private String resolveWorkShift(Long userId) {
        BinCollector collector = binCollectorRepository.findById(userId).orElse(null);
        if (collector == null) {
            log.warn("Unable to resolve work shift for userId {}. Duplicate-bin validation will be skipped.", userId);
            return null;
        }

        if (collector.getWorkShift() == null || collector.getWorkShift().isBlank()) {
            log.warn("Collector profile for userId {} does not define a work shift. Duplicate-bin validation will be skipped.", userId);
            return null;
        }

        return collector.getWorkShift().trim();
    }

    private RouteSessionSnapshotDTO validateShiftBinAvailability(
            RouteSessionCreateRequestDTO request,
            String workShift,
            String excludeSessionId,
            String trigger
    ) {
            if (workShift == null || workShift.isBlank()) {
                return null;
            }

        List<Long> selectedBinIds = request.getSelectedBinIds();
        if (selectedBinIds == null || selectedBinIds.isEmpty()) {
            return null;
        }

        List<String> overlappingSessions = new ArrayList<>();
        List<Long> overlappingBins = new ArrayList<>();

        for (RouteSessionState existing : sessionsById.values()) {
            if (existing.getSessionId().equals(excludeSessionId)) {
                continue;
            }
            if (existing.getWorkShift() == null || !existing.getWorkShift().equalsIgnoreCase(workShift)) {
                continue;
            }

            List<Long> existingBins = existing.getConfig() != null ? existing.getConfig().getSelectedBinIds() : Collections.emptyList();
            if (existingBins == null || existingBins.isEmpty()) {
                continue;
            }

            List<Long> conflicts = selectedBinIds.stream()
                    .filter(existingBins::contains)
                    .collect(Collectors.toList());

            if (!conflicts.isEmpty()) {
                overlappingSessions.add(existing.getSessionId());
                for (Long binId : conflicts) {
                    if (!overlappingBins.contains(binId)) {
                        overlappingBins.add(binId);
                    }
                }
            }
        }

        if (overlappingBins.isEmpty()) {
            return null;
        }

        String message = "WARNING: selected bin(s) " + overlappingBins + " are already assigned to another route in work shift " + workShift +
                (overlappingSessions.isEmpty() ? "" : " (conflicting session(s): " + overlappingSessions + ")");

        return RouteSessionSnapshotDTO.warning(
                excludeSessionId,
                request.getUserId(),
                0L,
                trigger,
                selectedBinIds,
                Collections.emptyList(),
                Collections.emptyList(),
                message
        );
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
        if (snapshot == null || snapshot.route == null || snapshot.userId == null) {
            return;
        }
        if (!"READY".equalsIgnoreCase(snapshot.status)) {
            return;
        }

        try {
            RouteUpdatePayload payload = toRouteUpdatePayload(snapshot);
            routeBroadcaster.onRouteOptimized(snapshot.userId, payload);
        } catch (Exception ignored) {
            // Keep route optimization resilient even if broadcasting fails.
        }
    }

    private RouteUpdatePayload toRouteUpdatePayload(RouteSessionSnapshotDTO snapshot) {
        Map<Integer, RouteUpdatePayload.VehicleRoute> mappedRoutes = new LinkedHashMap<>();

        if (snapshot.route.routes != null) {
            for (Map.Entry<Integer, VehicleRoute> entry : snapshot.route.routes.entrySet()) {
                VehicleRoute vr = entry.getValue();
                List<RouteUpdatePayload.BinStop> mappedStops = new ArrayList<>();

                if (vr.binSequence != null) {
                    for (BinStop stop : vr.binSequence) {
                        mappedStops.add(new RouteUpdatePayload.BinStop(
                                stop.stopOrder,
                                stop.binId,
                                stop.lat,
                                stop.lng,
                                stop.durationFromPrevStopSeconds,
                                null
                        ));
                    }
                }

                mappedRoutes.put(entry.getKey(), new RouteUpdatePayload.VehicleRoute(
                        vr.vehicleId,
                        vr.capacity,
                        vr.totalBins,
                        vr.estimatedDurationSeconds,
                        mappedStops
                ));
            }
        }

        long updatedAt = snapshot.generatedAt != null ? snapshot.generatedAt.toEpochMilli() : System.currentTimeMillis();

        return new RouteUpdatePayload(
                snapshot.sessionId,
                snapshot.userId,
                snapshot.route.totalVehiclesUsed,
                mappedRoutes,
                updatedAt
        );
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

}
