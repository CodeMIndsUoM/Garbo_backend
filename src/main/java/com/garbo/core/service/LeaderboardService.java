package com.garbo.core.service;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.GamificationTask;
import com.garbo.core.entity.Leaderboard;
import com.garbo.core.entity.ScoreTransaction;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.GamificationTaskRepository;
import com.garbo.core.repository.LeaderboardRepository;
import com.garbo.core.repository.ScoreTransactionRepository;
import com.garbo.core.repository.UserTaskProgressRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.event.ScoreAwardedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing leaderboard scores and rankings.
 * Handles points calculation and real-time leaderboard updates.
 */
@Slf4j
@Service
public class LeaderboardService {
    
    @Autowired
    private BinCollectorRepository binCollectorRepository;
    
    @Autowired
    private FieldMentorRepository fieldMentorRepository;
    
    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private GamificationTaskRepository gamificationTaskRepository;

    @Autowired
    private ScoreTransactionRepository scoreTransactionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTaskProgressRepository userTaskProgressRepository;
    
    /**
     * Award points to a collector for bin collection.
     * @param collectorId The collector's ID
     * @param pointsToAward Base points to award (will be modified by bin priority)
     * @param binPriority Priority level of the bin (HIGH, MEDIUM, LOW)
     */
    @Transactional
    public void awardPointsForBinCollection(Long collectorId, double pointsToAward, String binPriority) {
        // Leaderboard score is task-driven only. Bin collection updates progress,
        // but must not directly mutate reward points.
        log.debug("Skipping legacy bin scoring for collector {} (task-only scoring mode)", collectorId);
    }
    
    /**
     * Award points to a field mentor.
     */
    @Transactional
    public void awardPointsToFieldMentor(Long mentorId, double pointsToAward, String binPriority) {
        // Leaderboard score is task-driven only.
        log.debug("Skipping legacy bin scoring for field mentor {} (task-only scoring mode)", mentorId);
    }

    /**
     * Execute score award using admin-managed gamification task definitions.
     */
    @Transactional
    public boolean awardPointsForTaskCompletion(
            Long userId,
            String role,
            Long taskId,
            String sourceEventId,
            String reason,
            String priorityLevel
    ) {
        if (userId == null || role == null || taskId == null) {
            log.warn("Invalid task scoring request. userId={}, role={}, taskId={}", userId, role, taskId);
            return false;
        }

        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            log.warn("Gamification task not found: {}", taskId);
            return false;
        }

        GamificationTask task = taskOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (!task.matchesRole(role) || !task.isPublishedAndActive(now)) {
            log.warn("Task {} is not active for role {}", taskId, role);
            return false;
        }

        String normalizedSourceEventId = sourceEventId != null && !sourceEventId.isBlank()
                ? sourceEventId
                : UUID.randomUUID().toString();

        if (scoreTransactionRepository.existsByUserIdAndTaskIdAndSourceEventId(userId, taskId, normalizedSourceEventId)) {
            log.info("Skipping duplicate score transaction for user {} task {} source {}", userId, taskId, normalizedSourceEventId);
            return false;
        }

        double awardedPoints = task.getBasePoints() * resolvePriorityMultiplier(task, priorityLevel);

        if ("COLLECTOR".equalsIgnoreCase(role)) {
            Optional<BinCollector> collectorOpt = binCollectorRepository.findById(userId);
            if (collectorOpt.isEmpty()) {
                log.warn("Collector not found: {}", userId);
                return false;
            }
            BinCollector collector = collectorOpt.get();
            double before = collector.getRewardPoints();
            collector.setRewardPoints(before + awardedPoints);
            binCollectorRepository.save(collector);

            saveScoreTransaction(
                    collector.getEmpId(),
                    "COLLECTOR",
                    task,
                    task.getCode(),
                    awardedPoints,
                    before,
                    collector.getRewardPoints(),
                    normalizedSourceEventId,
                    reason != null ? reason : "Task completion"
            );
        } else if ("FIELD_MENTOR".equalsIgnoreCase(role)) {
            Optional<FieldMentor> mentorOpt = fieldMentorRepository.findById(userId);
            if (mentorOpt.isEmpty()) {
                log.warn("Field mentor not found: {}", userId);
                return false;
            }
            FieldMentor mentor = mentorOpt.get();
            double before = mentor.getRewardPoints();
            mentor.setRewardPoints(before + awardedPoints);
            fieldMentorRepository.save(mentor);

            saveScoreTransaction(
                    mentor.getEmpId(),
                    "FIELD_MENTOR",
                    task,
                    task.getCode(),
                    awardedPoints,
                    before,
                    mentor.getRewardPoints(),
                    normalizedSourceEventId,
                    reason != null ? reason : "Task completion"
            );
        } else {
            log.warn("Unsupported role for task scoring: {}", role);
            return false;
        }

        refreshLeaderboard();

        eventPublisher.publishEvent(new ScoreAwardedEvent(
                userId,
                role.toUpperCase(),
                taskId,
                normalizedSourceEventId
        ));
        return true;
    }
    
    /**
     * Get top leaderboard entries.
     */
    @Transactional
    public List<LeaderboardUpdatePayload.LeaderboardEntryDto> getTopLeaderboard(int limit) {
        return getTopLeaderboard(limit, null);
    }

    @Transactional
    public List<LeaderboardUpdatePayload.LeaderboardEntryDto> getTopLeaderboard(int limit, String roleFilter) {
        List<LeaderboardEntryTemp> allEntries = buildAllLeaderboardEntries(roleFilter);
        allEntries.sort((a, b) -> Double.compare(b.rewardPoints, a.rewardPoints));

        List<LeaderboardUpdatePayload.LeaderboardEntryDto> topEntries = new ArrayList<>();
        int topLimit = Math.max(0, limit);
        int count = Math.min(topLimit, allEntries.size());

        for (int index = 0; index < count; index++) {
            LeaderboardEntryTemp entry = allEntries.get(index);
            topEntries.add(toEntryDto(entry, index + 1));
        }

        return topEntries;
    }

    @Transactional
    public Optional<LeaderboardUpdatePayload.LeaderboardEntryDto> getUserLeaderboardEntry(
            Long userId,
            String roleFilter
    ) {
        if (userId == null) {
            return Optional.empty();
        }

        List<LeaderboardEntryTemp> allEntries = buildAllLeaderboardEntries(roleFilter);
        allEntries.sort((a, b) -> Double.compare(b.rewardPoints, a.rewardPoints));

        for (int index = 0; index < allEntries.size(); index++) {
            LeaderboardEntryTemp entry = allEntries.get(index);
            if (userId.equals(entry.userId)
                    && (roleFilter == null
                    || roleFilter.isBlank()
                    || roleFilter.equalsIgnoreCase(entry.role))) {
                return Optional.of(toEntryDto(entry, index + 1));
            }
        }

        return Optional.empty();
    }

    private List<LeaderboardEntryTemp> buildAllLeaderboardEntries(String roleFilter) {
        Map<String, Double> taskPointsByUserRole = scoreTransactionRepository
                .findTaskScoreAggregates()
                .stream()
                .collect(Collectors.toMap(
                        aggregate -> buildUserRoleKey(aggregate.getUserId(), aggregate.getRole()),
                        aggregate -> aggregate.getTotalPoints() != null ? aggregate.getTotalPoints() : 0.0,
                        Double::sum
                ));

        Map<Long, Double> completedTaskPointsByUser = userTaskProgressRepository
                .findCompletedPointsByUser()
                .stream()
                .collect(Collectors.toMap(
                        UserTaskProgressRepository.CompletedPointsAggregate::getUserId,
                        aggregate -> aggregate.getTotalPoints() != null ? aggregate.getTotalPoints() : 0.0,
                        Double::sum
                ));

        List<LeaderboardEntryTemp> allEntries = new ArrayList<>();
        Set<String> includedKeys = new HashSet<>();
        String normalizedRoleFilter = normalizeRoleFilter(roleFilter);

        if (normalizedRoleFilter == null || "COLLECTOR".equals(normalizedRoleFilter)) {
            for (BinCollector collector : binCollectorRepository.findAll()) {
                String key = buildUserRoleKey(collector.getEmpId(), "COLLECTOR");
                double rewardPoints = resolveAndPersistRewardPoints(
                        collector,
                        null,
                        taskPointsByUserRole.getOrDefault(key, 0.0),
                        completedTaskPointsByUser.getOrDefault(collector.getEmpId(), 0.0)
                );
                includedKeys.add(key);
                allEntries.add(new LeaderboardEntryTemp(
                        collector.getEmpId(),
                        collector.getEmpName(),
                        collector.getEmail(),
                        "COLLECTOR",
                        rewardPoints,
                        collector
                ));
            }
        }

        if (normalizedRoleFilter == null || "FIELD_MENTOR".equals(normalizedRoleFilter)) {
            for (FieldMentor mentor : fieldMentorRepository.findAll()) {
                String key = buildUserRoleKey(mentor.getEmpId(), "FIELD_MENTOR");
                double rewardPoints = resolveAndPersistRewardPoints(
                        null,
                        mentor,
                        taskPointsByUserRole.getOrDefault(key, 0.0),
                        completedTaskPointsByUser.getOrDefault(mentor.getEmpId(), 0.0)
                );
                includedKeys.add(key);
                allEntries.add(new LeaderboardEntryTemp(
                        mentor.getEmpId(),
                        mentor.getEmpName(),
                        mentor.getEmail(),
                        "FIELD_MENTOR",
                        rewardPoints,
                        mentor
                ));
            }
        }

        for (ScoreTransactionRepository.TaskScoreAggregate aggregate : scoreTransactionRepository.findTaskScoreAggregates()) {
            Long userId = aggregate.getUserId();
            String role = aggregate.getRole();
            String key = buildUserRoleKey(userId, role);
            if (includedKeys.contains(key)) {
                continue;
            }
            if (normalizedRoleFilter != null && !normalizedRoleFilter.equalsIgnoreCase(role)) {
                continue;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                continue;
            }

            double rewardPoints = Math.max(
                    aggregate.getTotalPoints() != null ? aggregate.getTotalPoints() : 0.0,
                    completedTaskPointsByUser.getOrDefault(userId, 0.0)
            );
            allEntries.add(new LeaderboardEntryTemp(
                    userId,
                    user.getEmpName(),
                    user.getEmail(),
                    role,
                    rewardPoints,
                    user
            ));
            includedKeys.add(key);
        }

        return allEntries;
    }

    private double resolveAndPersistRewardPoints(
            BinCollector collector,
            FieldMentor mentor,
            double transactionPoints,
            double progressPoints
    ) {
        double computed = Math.max(transactionPoints, progressPoints);
        if (collector != null) {
            double stored = collector.getRewardPoints();
            double resolved = Math.max(stored, computed);
            if (resolved > stored + 0.001) {
                collector.setRewardPoints(resolved);
                binCollectorRepository.save(collector);
            }
            return resolved;
        }
        if (mentor != null) {
            double stored = mentor.getRewardPoints();
            double resolved = Math.max(stored, computed);
            if (resolved > stored + 0.001) {
                mentor.setRewardPoints(resolved);
                fieldMentorRepository.save(mentor);
            }
            return resolved;
        }
        return computed;
    }

    private LeaderboardUpdatePayload.LeaderboardEntryDto toEntryDto(LeaderboardEntryTemp entry, int rank) {
        return new LeaderboardUpdatePayload.LeaderboardEntryDto(
                rank,
                entry.userId,
                entry.name,
                entry.rewardPoints,
                entry.role,
                null
        );
    }

    private String normalizeRoleFilter(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return null;
        }
        String normalized = roleFilter.trim().toUpperCase();
        if ("BIN_COLLECTOR".equals(normalized) || "COLLECTION_TEAM".equals(normalized)) {
            return "COLLECTOR";
        }
        if ("FIELD_STAFF".equals(normalized)) {
            return "FIELD_MENTOR";
        }
        return normalized;
    }

    private String buildUserRoleKey(Long userId, String role) {
        return userId + "::" + (role == null ? "" : role.toUpperCase());
    }
    
    /**
     * Refresh leaderboard rankings and persist today's snapshot to leaderboards table.
     * Should be called after any points update.
     */
    @Transactional
    public void refreshLeaderboard() {
        LocalDate today = LocalDate.now();

        List<LeaderboardEntryTemp> allEntries = buildAllLeaderboardEntries("COLLECTOR");
        allEntries.sort((a, b) -> Double.compare(b.rewardPoints, a.rewardPoints));

        Set<Long> activeUserIds = new HashSet<>();
        int rank = 1;
        for (LeaderboardEntryTemp entry : allEntries) {
            if (!(entry.userEntity instanceof BinCollector collector)) {
                continue;
            }

            activeUserIds.add(entry.userId);

            Leaderboard leaderboard = leaderboardRepository
                    .findFirstByUserIdAndRole(entry.userId, entry.role)
                    .orElseGet(Leaderboard::new);

            if (leaderboard.getVersion() == null) {
                leaderboard.setVersion(0L);
            }
            leaderboard.setRank(rank++);
            leaderboard.setUserId(entry.userId);
            leaderboard.setCollector(collector);
            leaderboard.setCollectorName(entry.name);
            leaderboard.setCollectorEmail(entry.email);
            leaderboard.setRewardPoints(entry.rewardPoints);
            leaderboard.setRole(entry.role);
            leaderboard.setSnapshotDate(today);
            leaderboard.setLastUpdated(LocalDateTime.now());

            leaderboardRepository.save(leaderboard);
        }

        for (Leaderboard stale : leaderboardRepository.findByRole("COLLECTOR")) {
            if (stale.getUserId() != null && !activeUserIds.contains(stale.getUserId())) {
                leaderboardRepository.delete(stale);
            }
        }

        log.info("Leaderboard table refreshed with {} collector entries for date {}", rank - 1, today);
    }
    
    /**
     * Convert Leaderboard entity to DTO for WebSocket transmission.
     */
    private LeaderboardUpdatePayload.LeaderboardEntryDto convertToDto(Leaderboard leaderboard) {
        return new LeaderboardUpdatePayload.LeaderboardEntryDto(
                leaderboard.getRank(),
                leaderboard.getUserId(),
                leaderboard.getCollectorName(),
                leaderboard.getRewardPoints(),
                leaderboard.getRole(),
                null  // rankChangeFromPrevious will be calculated later
        );
    }

    private double resolvePriorityMultiplier(GamificationTask task, String priorityLevel) {
        if (!"PRIORITY_WEIGHTED".equalsIgnoreCase(task.getScoringType())) {
            return 1.0;
        }
        if (priorityLevel == null) {
            return 1.0;
        }

        if ("HIGH".equalsIgnoreCase(priorityLevel)) {
            return task.getHighPriorityMultiplier();
        }
        if ("MEDIUM".equalsIgnoreCase(priorityLevel)) {
            return task.getMediumPriorityMultiplier();
        }
        return 1.0;
    }

    private void saveScoreTransaction(
            Long userId,
            String role,
            GamificationTask task,
            String taskCode,
            double pointsDelta,
            double scoreBefore,
            double scoreAfter,
            String sourceEventId,
            String reason
    ) {
        ScoreTransaction transaction = new ScoreTransaction();
        transaction.setUserId(userId);
        transaction.setRole(role);
        transaction.setTask(task);
        transaction.setTaskCode(taskCode);
        transaction.setPointsDelta(pointsDelta);
        transaction.setScoreBefore(scoreBefore);
        transaction.setScoreAfter(scoreAfter);
        transaction.setSourceEventId(sourceEventId);
        transaction.setReason(reason);
        transaction.setPeriodKey(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        scoreTransactionRepository.save(transaction);
    }
    
    /**
     * Temporary holder for leaderboard entry data during refresh.
     */
    private static class LeaderboardEntryTemp {
        Long userId;
        String name;
        String email;
        String role;
        double rewardPoints;
        Object userEntity;  // BinCollector or FieldMentor
        
        LeaderboardEntryTemp(Long userId, String name, String email, String role,
                           double rewardPoints, Object userEntity) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.rewardPoints = rewardPoints;
            this.userEntity = userEntity;
        }
    }
}
