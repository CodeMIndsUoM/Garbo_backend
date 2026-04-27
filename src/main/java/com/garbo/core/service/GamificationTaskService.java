package com.garbo.core.service;

import com.garbo.api.dto.gamification.AdminGamificationTaskUpsertRequest;
import com.garbo.core.entity.GamificationTask;
import com.garbo.core.repository.GamificationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GamificationTaskService {

    private static final String TASK_CODE_ROUTES_2_DAILY = "ROUTES_2_DAILY";
    private static final String TASK_CODE_ROUTES_3_DAILY = "ROUTES_3_DAILY";

    @Autowired
    private GamificationTaskRepository gamificationTaskRepository;

        @PostConstruct
        void ensureDefaultRouteTasks() {
        upsertDefaultRouteTask(
            TASK_CODE_ROUTES_2_DAILY,
            "2 routes in 1 day",
            "Complete 2 collection routes within a single day.",
            20.0,
            2.0
        );
        upsertDefaultRouteTask(
            TASK_CODE_ROUTES_3_DAILY,
            "3 routes in 1 day",
            "Complete 3 collection routes within a single day.",
            30.0,
            3.0
        );
        }

    public List<GamificationTask> getAllTasks() {
        return gamificationTaskRepository.findAll();
    }

    public Optional<GamificationTask> getTaskById(Long taskId) {
        return gamificationTaskRepository.findById(taskId);
    }

    public List<GamificationTask> getActiveTasksForRole(String role) {
        return gamificationTaskRepository.findActivePublishedTasksForRole(normalizeRole(role), LocalDateTime.now());
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "COLLECTOR";
        }

        String normalized = role.trim().toUpperCase();
        if ("BIN_COLLECTOR".equals(normalized) || "COLLECTION_TEAM".equals(normalized)) {
            return "COLLECTOR";
        }
        if ("FIELD_MENTOR".equals(normalized)) {
            return "FIELD_MENTOR";
        }
        if ("COLLECTOR".equals(normalized) || "ALL".equals(normalized)) {
            return normalized;
        }
        return normalized;
    }

    @Transactional
    public GamificationTask createTask(AdminGamificationTaskUpsertRequest request) {
        GamificationTask task = new GamificationTask();
        applyRequest(task, request, true);
        return gamificationTaskRepository.save(task);
    }

    @Transactional
    public Optional<GamificationTask> updateTask(Long taskId, AdminGamificationTaskUpsertRequest request) {
        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        GamificationTask task = taskOpt.get();
        applyRequest(task, request, false);
        return Optional.of(gamificationTaskRepository.save(task));
    }

    @Transactional
    public boolean deleteTask(Long taskId) {
        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        GamificationTask task = taskOpt.get();
        task.setStatus("ARCHIVED");
        gamificationTaskRepository.save(task);
        return true;
    }

    @Transactional
    public Optional<GamificationTask> updateTaskStatus(Long taskId, String status, Long adminId) {
        Optional<GamificationTask> taskOpt = gamificationTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }
        GamificationTask task = taskOpt.get();
        task.setStatus(status);
        task.setUpdatedByAdminId(adminId);
        return Optional.of(gamificationTaskRepository.save(task));
    }

    private void applyRequest(GamificationTask task, AdminGamificationTaskUpsertRequest request, boolean creating) {
        if (creating) {
            task.setCode(request.getCode());
        } else if (request.getCode() != null && !request.getCode().isBlank()) {
            task.setCode(request.getCode());
        }

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getRoleScope() != null && !request.getRoleScope().isBlank()) {
            task.setRoleScope(request.getRoleScope().toUpperCase());
        }
        if (request.getTaskType() != null && !request.getTaskType().isBlank()) {
            task.setTaskType(request.getTaskType());
        }
        if (request.getScoringType() != null && !request.getScoringType().isBlank()) {
            task.setScoringType(request.getScoringType().toUpperCase());
        }
        if (request.getBasePoints() > 0) {
            task.setBasePoints(request.getBasePoints());
        }
        if (request.getTargetProgress() != null && request.getTargetProgress() > 0) {
            task.setTargetProgress(request.getTargetProgress());
        }
        if (request.getHighPriorityMultiplier() != null) {
            task.setHighPriorityMultiplier(request.getHighPriorityMultiplier());
        }
        if (request.getMediumPriorityMultiplier() != null) {
            task.setMediumPriorityMultiplier(request.getMediumPriorityMultiplier());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            task.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getStartAt() != null) {
            task.setStartAt(request.getStartAt());
        }
        if (request.getEndAt() != null) {
            task.setEndAt(request.getEndAt());
        }

        if (creating) {
            task.setCreatedByAdminId(request.getAdminId());
        }
        task.setUpdatedByAdminId(request.getAdminId());
    }

    private void upsertDefaultRouteTask(
            String code,
            String title,
            String description,
            double basePoints,
            double targetProgress
    ) {
        GamificationTask task = gamificationTaskRepository.findByCode(code)
                .orElseGet(GamificationTask::new);

        task.setCode(code);
        task.setTitle(title);
        task.setDescription(description);
        task.setRoleScope("COLLECTOR");
        task.setTaskType("DAILY_ROUTE_COMPLETION");
        task.setScoringType("FIXED");
        task.setBasePoints(basePoints);
        task.setTargetProgress(targetProgress);
        task.setStatus("PUBLISHED");
        task.setStartAt(null);
        task.setEndAt(null);

        if (task.getHighPriorityMultiplier() <= 0) {
            task.setHighPriorityMultiplier(1.5);
        }
        if (task.getMediumPriorityMultiplier() <= 0) {
            task.setMediumPriorityMultiplier(1.2);
        }

        gamificationTaskRepository.save(task);
    }
}
