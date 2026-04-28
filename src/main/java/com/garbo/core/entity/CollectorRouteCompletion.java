package com.garbo.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collector_route_completions", indexes = {
        @Index(name = "idx_route_completion_collector", columnList = "collector_id,completed_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_route_completion_collector_session", columnNames = {"collector_id", "session_id"})
})
public class CollectorRouteCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collector_id", nullable = false)
    private Long collectorId;

    @Column(name = "session_id", nullable = false, length = 120)
    private String sessionId;

    @Column(name = "assigned_bins", nullable = false)
    private int assignedBins;

    @Column(name = "collected_bins", nullable = false)
    private int collectedBins;

    @Column(name = "missed_bins", nullable = false)
    private int missedBins;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
}
