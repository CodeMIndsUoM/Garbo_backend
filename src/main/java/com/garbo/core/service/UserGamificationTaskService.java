package com.garbo.core.service;

import com.garbo.api.dto.gamification.UserGamificationTaskProgressResponse;
import com.garbo.core.entity.GamificationTask;
import com.garbo.core.entity.UserTaskProgress;
import com.garbo.core.repository.UserTaskProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserGamificationTaskService {

    @Autowired
    private GamificationTaskService gamificationTaskService;

    @Autowired
    private UserTaskProgressRepository userTaskProgressRepository;

    @Transactional
    public List<UserGamificationTaskProgressResponse> getUserTaskProgress(Long userId, String role) {
        List<GamificationTask> activeTasks = gamificationTaskService.getActiveTasksForRole(role);
        List<UserTaskProgress> savedProgressList = userTaskProgressRepository.findByUserId(userId);

        Map<Long, UserTaskProgress> progressByTaskId = new HashMap<>();
        for (UserTaskProgress progress : savedProgressList) {
            if (progress.getTask() == null || progress.getTask().getId() == null) {
                continue;
            }
            progressByTaskId.put(progress.getTask().getId(), progress);
        }

        List<UserGamificationTaskProgressResponse> result = new ArrayList<>();
        for (GamificationTask task : activeTasks) {
            UserTaskProgress progress = progressByTaskId.get(task.getId());
            if (progress != null && resetDailyProgressIfNeeded(task, progress)) {
                progress = userTaskProgressRepository.save(progress);
            }
            double targetProgress = progress != null && progress.getTargetProgress() > 0
                    ? progress.getTargetProgress()
                    : (task.getTargetProgress() != null && task.getTargetProgress() > 0
                            ? task.getTargetProgress()
                            : 1.0);
            double currentProgress = progress != null ? progress.getCurrentProgress() : 0.0;
            boolean isCompleted = progress != null && progress.isCompleted();
            double pointsEarned = progress != null ? progress.getPointsEarned() : 0.0;
            double availablePoints = task.getBasePoints();
            boolean isNew = progress == null || (currentProgress <= 0 && !isCompleted);
            LocalDateTime completedAt = progress != null ? progress.getCompletedAt() : null;

            result.add(new UserGamificationTaskProgressResponse(
                    userId,
                    task.getId(),
                    task.getCode(),
                    task.getTitle(),
                    task.getDescription(),
                    availablePoints,
                    currentProgress,
                    targetProgress,
                    isCompleted,
                    isNew,
                    completedAt != null ? completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    pointsEarned
            ));
        }

        result.sort((a, b) -> {
            if (a.isCompleted() == b.isCompleted()) {
                return a.getTaskTitle().compareToIgnoreCase(b.getTaskTitle());
            }
            return a.isCompleted() ? -1 : 1;
        });

        return result;
    }

    private boolean resetDailyProgressIfNeeded(GamificationTask task, UserTaskProgress progress) {
        if (task == null || progress == null) {
            return false;
        }
        if (!isDailyResetTask(task)) {
            return false;
        }

        LocalDate today = LocalDate.now();

        if (progress.getCompletedAt() != null && !progress.getCompletedAt().toLocalDate().isEqual(today)) {
            progress.setCurrentProgress(0.0);
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress.setPointsEarned(0.0);
            return true;
        }

        if (!progress.isCompleted()
                && progress.getUpdatedAt() != null
                && !progress.getUpdatedAt().toLocalDate().isEqual(today)
                && progress.getCurrentProgress() > 0) {
            progress.setCurrentProgress(0.0);
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress.setPointsEarned(0.0);
            return true;
        }

        return false;
    }

    private boolean isDailyResetTask(GamificationTask task) {
        String taskType = task.getTaskType() != null ? task.getTaskType().trim().toUpperCase() : "";
        String code = task.getCode() != null ? task.getCode().trim().toUpperCase() : "";
        return "ACTIVE_BIN_DAILY".equals(taskType)
                || "DAILY_ACTIVE_BINS".equals(taskType)
                || "ACTIVE_BINS_DAILY".equals(taskType)
                || "DAILY_BIN_REPORT".equals(taskType)
                || code.contains("DAILY");
    }
}
