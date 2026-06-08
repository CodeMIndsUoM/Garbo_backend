package com.garbo.core.service;

import com.garbo.core.entity.GamificationTask;
import com.garbo.core.entity.UserTaskProgress;
import com.garbo.core.repository.GamificationTaskRepository;
import com.garbo.core.repository.UserTaskProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserTaskProgressService {

    @Autowired
    private GamificationTaskRepository gamificationTaskRepository;

    @Autowired
    private UserTaskProgressRepository userTaskProgressRepository;

    @Autowired
    private GamificationTaskService gamificationTaskService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Transactional
    public Optional<UserTaskProgress> incrementTaskProgress(
            Long userId,
            String role,
            Long taskId,
            String sourceEventId,
            String reason,
            String priorityLevel,
            Double progressIncrement
    ) {
        if (userId == null || role == null || taskId == null) {
            return Optional.empty();
        }

        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        GamificationTask task = taskOpt.get();
        if (!task.isPublishedAndActive(LocalDateTime.now()) || !task.matchesRole(normalizeRole(role))) {
            return Optional.empty();
        }

        UserTaskProgress progress = userTaskProgressRepository
                .findByUserIdAndTaskId(userId, taskId)
                .orElseGet(() -> {
                    UserTaskProgress created = new UserTaskProgress();
                    created.setUserId(userId);
                    created.setTask(task);
                    created.setCurrentProgress(0.0);
                    double taskTarget = task.getTargetProgress() != null && task.getTargetProgress() > 0
                            ? task.getTargetProgress()
                            : 1.0;
                    created.setTargetProgress(taskTarget);
                    created.setCompleted(false);
                    created.setPointsEarned(0.0);
                    return created;
                });

                resetDailyProgressIfNeeded(task, progress);

        double increment = progressIncrement != null && progressIncrement > 0 ? progressIncrement : 1.0;
        if (!progress.isCompleted()) {
            double updated = Math.min(progress.getCurrentProgress() + increment, progress.getTargetProgress());
            progress.setCurrentProgress(updated);

            if (updated >= progress.getTargetProgress()) {
                String completionEventId = sourceEventId != null && !sourceEventId.isBlank()
                        ? sourceEventId
                        : "task-complete-" + userId + "-" + taskId;

                boolean awarded = leaderboardService.awardPointsForTaskCompletion(
                        userId,
                        normalizeRole(role),
                        taskId,
                        completionEventId,
                        reason,
                        priorityLevel
                );

                if (awarded) {
                    progress.setCompleted(true);
                    progress.setCompletedAt(LocalDateTime.now());
                    progress.setPointsEarned(
                            task.getBasePoints() * resolvePriorityMultiplier(task, priorityLevel)
                    );
                }
            }
        }

        UserTaskProgress saved = userTaskProgressRepository.save(progress);
        return Optional.of(saved);
    }

    private void resetDailyProgressIfNeeded(GamificationTask task, UserTaskProgress progress) {
        if (task == null || progress == null) {
            return;
        }
        if (!isDailyResetTask(task)) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (progress.getCompletedAt() != null && !progress.getCompletedAt().toLocalDate().isEqual(today)) {
            progress.setCurrentProgress(0.0);
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress.setPointsEarned(0.0);
            return;
        }

        if (!progress.isCompleted()
                && progress.getUpdatedAt() != null
                && !progress.getUpdatedAt().toLocalDate().isEqual(today)
                && progress.getCurrentProgress() > 0) {
            progress.setCurrentProgress(0.0);
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress.setPointsEarned(0.0);
        }
    }

    private boolean isDailyResetTask(GamificationTask task) {
        String taskType = task.getTaskType() != null ? task.getTaskType().trim().toUpperCase() : "";
        String code = task.getCode() != null ? task.getCode().trim().toUpperCase() : "";
        return "ACTIVE_BIN_DAILY".equals(taskType)
                || "DAILY_ACTIVE_BINS".equals(taskType)
                || "ACTIVE_BINS_DAILY".equals(taskType)
                || code.contains("DAILY");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "COLLECTOR";
        }
        String normalized = role.trim().toUpperCase();
        if ("BIN_COLLECTOR".equals(normalized) || "COLLECTION_TEAM".equals(normalized)) {
            return "COLLECTOR";
        }
        return normalized;
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

    @Transactional
    public List<UserTaskProgress> incrementActiveBinTasksForCollection(
            Long userId,
            String role,
            Long binId,
            String priorityLevel,
            String sessionId
    ) {
        String normalizedRole = normalizeRole(role);
        List<GamificationTask> activeTasks = gamificationTaskService.getActiveTasksForRole(normalizedRole);
        List<UserTaskProgress> updatedProgress = new ArrayList<>();

        for (GamificationTask task : activeTasks) {
            if (!isActiveBinCollectionTask(task)) {
                continue;
            }

            String sourceEventId = buildSourceEventId(userId, task.getId(), binId, sessionId);
            Optional<UserTaskProgress> progress = incrementTaskProgress(
                    userId,
                    normalizedRole,
                    task.getId(),
                    sourceEventId,
                    "Active bin collected",
                    priorityLevel,
                    1.0
            );
            progress.ifPresent(updatedProgress::add);
        }

        return updatedProgress;
    }

    @Transactional
    public List<UserTaskProgress> incrementDailyRouteTasksForCompletion(
            Long userId,
            String role,
            String sessionId
    ) {
        String normalizedRole = normalizeRole(role);
        List<GamificationTask> activeTasks = gamificationTaskService.getActiveTasksForRole(normalizedRole);
        List<UserTaskProgress> updatedProgress = new ArrayList<>();

        for (GamificationTask task : activeTasks) {
            if (!isDailyRouteCompletionTask(task)) {
                continue;
            }

            String sourceEventId = buildRouteSourceEventId(userId, task.getId(), sessionId);
            Optional<UserTaskProgress> progress = incrementTaskProgress(
                    userId,
                    normalizedRole,
                    task.getId(),
                    sourceEventId,
                    "Route completed",
                    null,
                    1.0
            );
            progress.ifPresent(updatedProgress::add);
        }

        return updatedProgress;
    }

    @Transactional
    public List<UserTaskProgress> incrementFieldMentorReportTasks(Long userId, Long binId) {
        String normalizedRole = "FIELD_MENTOR";
        List<GamificationTask> activeTasks = gamificationTaskService.getActiveTasksForRole(normalizedRole);
        List<UserTaskProgress> updatedProgress = new ArrayList<>();

        for (GamificationTask task : activeTasks) {
            if (!isFieldMentorReportTask(task)) {
                continue;
            }

            String sourceEventId = "mentor-report-" + userId + "-" + task.getId() + "-" + binId + "-" + LocalDate.now();
            Optional<UserTaskProgress> progress = incrementTaskProgress(
                    userId,
                    normalizedRole,
                    task.getId(),
                    sourceEventId,
                    "Bin reported",
                    null,
                    1.0
            );
            progress.ifPresent(updatedProgress::add);
        }

        return updatedProgress;
    }

    private boolean isFieldMentorReportTask(GamificationTask task) {
        String taskType = task.getTaskType() != null ? task.getTaskType().trim().toUpperCase() : "";
        return "BIN_REPORT".equals(taskType)
                || "FIELD_MENTOR_REPORT".equals(taskType)
                || "DAILY_BIN_REPORT".equals(taskType);
    }

    private boolean isActiveBinCollectionTask(GamificationTask task) {
        String taskType = task.getTaskType() != null ? task.getTaskType().trim().toUpperCase() : "";
        return "ACTIVE_BIN_DAILY".equals(taskType)
                || "BIN_COLLECTION".equals(taskType);
    }

    private boolean isDailyRouteCompletionTask(GamificationTask task) {
        String taskType = task.getTaskType() != null ? task.getTaskType().trim().toUpperCase() : "";
        return "DAILY_ROUTE_COMPLETION".equals(taskType)
                || "ROUTE_COMPLETION".equals(taskType);
    }

    private String buildSourceEventId(Long userId, Long taskId, Long binId, String sessionId) {
        String sessionSegment = sessionId != null && !sessionId.isBlank() ? sessionId : "no-session";
        return "bin-progress-" + userId + "-" + taskId + "-" + binId + "-" + sessionSegment + "-" + System.nanoTime();
    }

    private String buildRouteSourceEventId(Long userId, Long taskId, String sessionId) {
        String sessionSegment = sessionId != null && !sessionId.isBlank() ? sessionId : "no-session";
        return "route-progress-" + userId + "-" + taskId + "-" + sessionSegment + "-" + LocalDate.now();
    }
}
