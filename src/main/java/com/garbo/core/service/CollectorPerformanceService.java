package com.garbo.core.service;

import com.garbo.api.dto.performance.CollectorPerformanceStatsResponse;
import com.garbo.api.dto.performance.DailyPerformancePoint;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.CollectorRouteCompletion;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.CollectorRouteCompletionRepository;
import com.garbo.core.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CollectorPerformanceService {

    @Autowired
    private BinCollectorRepository binCollectorRepository;

    @Autowired
    private CollectorRouteCompletionRepository collectorRouteCompletionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CollectorRouteCompletion recordRouteCompletion(
            Long collectorId,
            String sessionId,
            Integer assignedBins,
            Integer collectedBins,
            Integer missedBins,
            Long durationSeconds,
            LocalDateTime completedAt
    ) {
        if (collectorId == null || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("collectorId and sessionId are required");
        }

        resolveCollectorOrCreateIfAllowed(collectorId);

        CollectorRouteCompletion completion = collectorRouteCompletionRepository
                .findByCollectorIdAndSessionId(collectorId, sessionId)
                .orElseGet(CollectorRouteCompletion::new);

        completion.setCollectorId(collectorId);
        completion.setSessionId(sessionId);
        completion.setAssignedBins(Math.max(0, assignedBins != null ? assignedBins : 0));
        completion.setCollectedBins(Math.max(0, collectedBins != null ? collectedBins : 0));
        completion.setMissedBins(Math.max(0, missedBins != null ? missedBins : 0));
        completion.setDurationSeconds(Math.max(0, durationSeconds != null ? durationSeconds : 0));
        completion.setCompletedAt(completedAt != null ? completedAt : LocalDateTime.now());

        return collectorRouteCompletionRepository.save(completion);
    }

    @Transactional
    public CollectorPerformanceStatsResponse getCollectorPerformanceStats(Long userId) {
        BinCollector collector = resolveCollectorOrCreateIfAllowed(userId);
        if (collector == null) {
            User user = userRepository.findById(userId).orElse(null);
            return new CollectorPerformanceStatsResponse(
                    userId,
                    resolveFromDate(user, List.of()),
                    0,
                    0,
                    0.0,
                    0.0,
                    0,
                    0,
                    List.of()
            );
        }

        List<CollectorRouteCompletion> completions = collectorRouteCompletionRepository
                .findByCollectorIdOrderByCompletedAtAsc(userId);

        int totalCollected = Math.max(0, collector.getCompletedCollections());
        int collectorMissed = Math.max(0, collector.getMissedCollections());

        int routesDone = completions.size();
        long totalDurationSeconds = 0L;
        int collectedFromCompletions = 0;
        int assignedFromCompletions = 0;
        int missedFromCompletions = 0;

        for (CollectorRouteCompletion completion : completions) {
            totalDurationSeconds += Math.max(0L, completion.getDurationSeconds());
            collectedFromCompletions += Math.max(0, completion.getCollectedBins());
            assignedFromCompletions += Math.max(0, completion.getAssignedBins());
            missedFromCompletions += Math.max(0, completion.getMissedBins());
        }

        int collectedForEfficiency = assignedFromCompletions > 0
                ? collectedFromCompletions
                : totalCollected;

        int assignedBinsTotal = assignedFromCompletions > 0
                ? assignedFromCompletions
                : totalCollected + collectorMissed;
        int impliedMissedFromAssigned = Math.max(0, assignedBinsTotal - collectedForEfficiency);
        int missedBinsTotal = assignedFromCompletions > 0
                ? missedFromCompletions
                : collectorMissed;
        missedBinsTotal = Math.max(missedBinsTotal, impliedMissedFromAssigned);

        double averageRouteTimeSeconds = routesDone > 0
                ? (double) totalDurationSeconds / (double) routesDone
                : 0.0;

        double efficiencyPercent = calculateEfficiency(collectedForEfficiency, assignedBinsTotal, missedBinsTotal);

        User user = userRepository.findById(userId).orElse(null);
        String fromDate = resolveFromDate(user, completions);

        List<DailyPerformancePoint> series = buildTimeSeries(completions);

        return new CollectorPerformanceStatsResponse(
                userId,
                fromDate,
                totalCollected,
                routesDone,
                averageRouteTimeSeconds,
                efficiencyPercent,
                assignedBinsTotal,
                missedBinsTotal,
                series
        );
    }

    private String resolveFromDate(User user, List<CollectorRouteCompletion> completions) {
        if (user != null && user.getCreatedAt() != null) {
            return user.getCreatedAt().toLocalDate().toString();
        }
        if (!completions.isEmpty()) {
            return completions.get(0).getCompletedAt().toLocalDate().toString();
        }
        return LocalDate.now().toString();
    }

    private List<DailyPerformancePoint> buildTimeSeries(List<CollectorRouteCompletion> completions) {
        Map<LocalDate, DailyAccumulator> byDay = new LinkedHashMap<>();

        for (CollectorRouteCompletion completion : completions) {
            LocalDate day = completion.getCompletedAt().toLocalDate();
            DailyAccumulator accumulator = byDay.computeIfAbsent(day, ignored -> new DailyAccumulator());
            accumulator.routes += 1;
            accumulator.collected += Math.max(0, completion.getCollectedBins());
            accumulator.assigned += Math.max(0, completion.getAssignedBins());
            accumulator.missed += Math.max(0, completion.getMissedBins());
            accumulator.durationSeconds += Math.max(0L, completion.getDurationSeconds());
        }

        List<DailyPerformancePoint> points = new ArrayList<>();
        int runningCollected = 0;
        int runningRoutes = 0;
        int runningAssigned = 0;
        int runningMissed = 0;
        long runningDuration = 0L;

        for (Map.Entry<LocalDate, DailyAccumulator> entry : byDay.entrySet()) {
            DailyAccumulator accumulator = entry.getValue();
            runningCollected += accumulator.collected;
            runningRoutes += accumulator.routes;
            runningAssigned += accumulator.assigned;
            runningMissed += accumulator.missed;
            runningDuration += accumulator.durationSeconds;

                int impliedMissed = Math.max(0, runningAssigned - runningCollected);
                int effectiveMissed = Math.max(runningMissed, impliedMissed);

            double avgSeconds = runningRoutes > 0
                    ? (double) runningDuration / (double) runningRoutes
                    : 0.0;
                double efficiency = calculateEfficiency(runningCollected, runningAssigned, effectiveMissed);

            points.add(new DailyPerformancePoint(
                    entry.getKey().toString(),
                    runningCollected,
                    runningRoutes,
                    avgSeconds,
                    efficiency,
                    runningAssigned,
                    effectiveMissed
            ));
        }

        return points;
    }

    private double calculateEfficiency(int totalCollected, int assignedTotal, int missedTotal) {
        int collectedSafe = Math.max(0, totalCollected);
        int missedSafe = Math.max(0, missedTotal);

        // Ensure missed bins always reduce efficiency even when assigned count is under-reported.
        int effectiveAssigned = Math.max(Math.max(0, assignedTotal), collectedSafe + missedSafe);
        if (effectiveAssigned == 0) {
            return 0.0;
        }

        double ratio = ((double) collectedSafe / (double) effectiveAssigned) * 100.0;
        if (ratio < 0) {
            return 0.0;
        }
        return Math.min(100.0, ratio);
    }

    private BinCollector resolveCollectorOrCreateIfAllowed(Long userId) {
        if (userId == null) {
            return null;
        }

        var existing = binCollectorRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        if (!isCollectorRole(user.getRole())) {
            log.warn("Skipping collector auto-provision for non-collector role. userId={}, role={}", userId, user.getRole());
            return null;
        }

        BinCollector created = new BinCollector();
        created.setEmpId(user.getEmpId());
        created.setEmpName(user.getEmpName());
        created.setEmail(user.getEmail());
        created.setPassword(user.getPassword());
        created.setRole(user.getRole());
        created.setPhone(user.getPhone());
        created.setCreatedAt(user.getCreatedAt());
        created.setLastLoginAt(user.getLastLoginAt());
        created.setAssignedZone(null);
        created.setTeam(null);
        created.setWorkShift(null);
        created.setOnDuty(false);
        created.setCompletedCollections(0);
        created.setMissedCollections(0);
        created.setRewardPoints(0.0);

        log.info("Auto-provisioning missing collector profile for userId={}", userId);
        return binCollectorRepository.save(created);
    }

    private boolean isCollectorRole(String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        String normalized = role.trim().toUpperCase();
        return "COLLECTOR".equals(normalized)
                || "BIN_COLLECTOR".equals(normalized)
                || "COLLECTION_TEAM".equals(normalized);
    }

    private static class DailyAccumulator {
        int routes;
        int collected;
        int assigned;
        int missed;
        long durationSeconds;
    }
}
