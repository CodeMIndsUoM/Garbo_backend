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
@Table(name = "score_transactions", indexes = {
        @Index(name = "idx_score_tx_user", columnList = "user_id,created_at"),
        @Index(name = "idx_score_tx_task", columnList = "task_id"),
        @Index(name = "idx_score_tx_period", columnList = "period_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_score_tx_task_event", columnNames = {"user_id", "task_id", "source_event_id"})
})
public class ScoreTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private GamificationTask task;

    @Column(name = "task_code", length = 100)
    private String taskCode;

    @Column(name = "points_delta", nullable = false)
    private double pointsDelta;

    @Column(name = "score_before", nullable = false)
    private double scoreBefore;

    @Column(name = "score_after", nullable = false)
    private double scoreAfter;

    @Column(name = "source_event_id", length = 120)
    private String sourceEventId;

    @Column(name = "period_key", length = 40)
    private String periodKey;

    @Column(name = "reason", length = 250)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
