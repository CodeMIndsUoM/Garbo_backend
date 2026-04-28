package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gamification_tasks", indexes = {
        @Index(name = "idx_gam_task_status", columnList = "status"),
        @Index(name = "idx_gam_task_role", columnList = "role_scope"),
        @Index(name = "idx_gam_task_schedule", columnList = "start_at,end_at")
})
public class GamificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "role_scope", nullable = false, length = 30)
    private String roleScope; // COLLECTOR | FIELD_MENTOR | ALL

    @Column(name = "task_type", nullable = false, length = 80)
    private String taskType;

    @Column(name = "scoring_type", nullable = false, length = 40)
    private String scoringType; // FIXED | PRIORITY_WEIGHTED

    @Column(name = "base_points", nullable = false)
    private double basePoints;

    @Column(name = "target_progress")
    private Double targetProgress = 1.0;

    @Column(name = "high_priority_multiplier", nullable = false)
    private double highPriorityMultiplier = 1.5;

    @Column(name = "medium_priority_multiplier", nullable = false)
    private double mediumPriorityMultiplier = 1.2;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // DRAFT | PUBLISHED | PAUSED | ARCHIVED

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (scoringType == null || scoringType.isBlank()) {
            scoringType = "PRIORITY_WEIGHTED";
        }
        if (roleScope == null || roleScope.isBlank()) {
            roleScope = "COLLECTOR";
        }
        if (targetProgress == null || targetProgress <= 0) {
            targetProgress = 1.0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean matchesRole(String role) {
        return "ALL".equalsIgnoreCase(roleScope) || roleScope.equalsIgnoreCase(role);
    }

    public boolean isPublishedAndActive(LocalDateTime now) {
        if (!"PUBLISHED".equalsIgnoreCase(status)) {
            return false;
        }
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        return endAt == null || !now.isAfter(endAt);
    }
}
