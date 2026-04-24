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
    public void awardPointsForTaskCompletion(
            Long userId,
            String role,
            Long taskId,
            String sourceEventId,
            String reason,
            String priorityLevel
    ) {
        if (userId == null || role == null || taskId == null) {
            log.warn("Invalid task scoring request. userId={}, role={}, taskId={}", userId, role, taskId);
            return;
        }

        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            log.warn("Gamification task not found: {}", taskId);
            return;
        }

        GamificationTask task = taskOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (!task.matchesRole(role) || !task.isPublishedAndActive(now)) {
            log.warn("Task {} is not active for role {}", taskId, role);
            return;
        }

        String normalizedSourceEventId = sourceEventId != null && !sourceEventId.isBlank()
                ? sourceEventId
                : UUID.randomUUID().toString();

        if (scoreTransactionRepository.existsByUserIdAndTaskIdAndSourceEventId(userId, taskId, normalizedSourceEventId)) {
            log.info("Skipping duplicate score transaction for user {} task {} source {}", userId, taskId, normalizedSourceEventId);
            return;
        }

        String periodKey = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        if (scoreTransactionRepository.existsByUserIdAndTaskIdAndPeriodKey(userId, taskId, periodKey)) {
            log.info("Skipping same-day duplicate score transaction for user {} task {} period {}", userId, taskId, periodKey);
            return;
        }

        double awardedPoints = task.getBasePoints() * resolvePriorityMultiplier(task, priorityLevel);

        if ("COLLECTOR".equalsIgnoreCase(role)) {
            Optional<BinCollector> collectorOpt = binCollectorRepository.findById(userId);
            if (collectorOpt.isEmpty()) {
                log.warn("Collector not found: {}", userId);
                return;
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
                return;
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
            return;
        }

        // Do not refresh leaderboard snapshot here because current DB schema enforces
        // leaderboards.collector_id NOT NULL and breaks mixed-role snapshot writes.
        // Emit realtime event without forcing snapshot rewrite so task progress + score persist safely.
        eventPublisher.publishEvent(new ScoreAwardedEvent(
                userId,
                role.toUpperCase(),
                taskId,
                normalizedSourceEventId
        ));
    }
    
    /**
     * Get top leaderboard entries.
     */
    public List<LeaderboardUpdatePayload.LeaderboardEntryDto> getTopLeaderboard(int limit) {
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

        for (BinCollector collector : binCollectorRepository.findAll()) {
            String key = buildUserRoleKey(collector.getEmpId(), "COLLECTOR");
            double taskOnlyPoints = taskPointsByUserRole.getOrDefault(
                    key,
                    completedTaskPointsByUser.getOrDefault(collector.getEmpId(), 0.0)
            );
            includedKeys.add(key);
            allEntries.add(new LeaderboardEntryTemp(
                    collector.getEmpId(),
                    collector.getEmpName(),
                    collector.getEmail(),
                    "COLLECTOR",
                    taskOnlyPoints,
                    collector
            ));
        }

        for (FieldMentor mentor : fieldMentorRepository.findAll()) {
            String key = buildUserRoleKey(mentor.getEmpId(), "FIELD_MENTOR");
            double taskOnlyPoints = taskPointsByUserRole.getOrDefault(
                    key,
                    completedTaskPointsByUser.getOrDefault(mentor.getEmpId(), 0.0)
            );
            includedKeys.add(key);
            allEntries.add(new LeaderboardEntryTemp(
                    mentor.getEmpId(),
                    mentor.getEmpName(),
                    mentor.getEmail(),
                    "FIELD_MENTOR",
                    taskOnlyPoints,
                    mentor
            ));
        }

        // Include users who have task score transactions but no collector/mentor profile row.
        for (ScoreTransactionRepository.TaskScoreAggregate aggregate : scoreTransactionRepository.findTaskScoreAggregates()) {
            Long userId = aggregate.getUserId();
            String role = aggregate.getRole();
            String key = buildUserRoleKey(userId, role);
            if (includedKeys.contains(key)) {
                continue;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                continue;
            }

            allEntries.add(new LeaderboardEntryTemp(
                    userId,
                    user.getEmpName(),
                    user.getEmail(),
                    role,
                    Math.max(
                        aggregate.getTotalPoints() != null ? aggregate.getTotalPoints() : 0.0,
                        completedTaskPointsByUser.getOrDefault(userId, 0.0)
                    ),
                    user
            ));
            includedKeys.add(key);
        }

        allEntries.sort((a, b) -> Double.compare(b.rewardPoints, a.rewardPoints));

        List<LeaderboardUpdatePayload.LeaderboardEntryDto> topEntries = new ArrayList<>();
        int topLimit = Math.max(0, limit);
        int count = Math.min(topLimit, allEntries.size());

        for (int index = 0; index < count; index++) {
            LeaderboardEntryTemp entry = allEntries.get(index);
            topEntries.add(new LeaderboardUpdatePayload.LeaderboardEntryDto(
                    index + 1,
                    entry.userId,
                    entry.name,
                    entry.rewardPoints,
                    entry.role,
                    null
            ));
        }

        return topEntries;
    }

    private String buildUserRoleKey(Long userId, String role) {
        return userId + "::" + (role == null ? "" : role.toUpperCase());
    }
    
    /**
     * Refresh leaderboard rankings.
     * Should be called after any points update.
     */
    @Transactional
    public void refreshLeaderboard() {
        try {
            LocalDate today = LocalDate.now();
            
            // Clear old entries for today
            List<Leaderboard> todayEntries = leaderboardRepository.findBySnapshotDate(today);
            leaderboardRepository.deleteAll(todayEntries);
            
            // Collect active collectors.
            // Note: current production schema enforces leaderboards.collector_id NOT NULL,
            // so persisting non-collector leaderboard rows is unsafe.
            List<BinCollector> collectors = binCollectorRepository.findAll();
            
            // Create sorted list with collector data
            List<LeaderboardEntryTemp> allEntries = new ArrayList<>();
            
            for (BinCollector collector : collectors) {
                allEntries.add(new LeaderboardEntryTemp(
                        collector.getEmpId(),
                        collector.getEmpName(),
                        collector.getEmail(),
                        "COLLECTOR",
                        collector.getRewardPoints(),
                        collector
                ));
            }
            
            // Sort by reward points (descending)
            allEntries.sort((a, b) -> Double.compare(b.rewardPoints, a.rewardPoints));
            
            // Create and save leaderboard entities with ranks
            for (int rank = 0; rank < allEntries.size(); rank++) {
                LeaderboardEntryTemp entry = allEntries.get(rank);
                
                Leaderboard leaderboard = new Leaderboard();
                leaderboard.setRank(rank + 1);
                leaderboard.setUserId(entry.userId);
                leaderboard.setCollectorName(entry.name);
                leaderboard.setCollectorEmail(entry.email);
                leaderboard.setRewardPoints(entry.rewardPoints);
                leaderboard.setRole(entry.role);
                leaderboard.setSnapshotDate(today);
                leaderboard.setLastUpdated(LocalDateTime.now());
                
                leaderboard.setCollector((BinCollector) entry.userEntity);
                
                leaderboardRepository.save(leaderboard);
            }
            
            log.info("Leaderboard refreshed with {} entries for date {}", allEntries.size(), today);
            
        } catch (Exception e) {
            log.error("Error refreshing leaderboard: {}", e.getMessage());
        }
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
