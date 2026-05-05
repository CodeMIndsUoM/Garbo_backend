package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "route_sessions")
public class RouteSession {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private java.util.UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PROCESSING | READY | ERROR

    @Column(name = "trigger", length = 50)
    private String trigger;

    @Column(name = "version")
    private Long version = 0L;

    /**
     * Stores the original admin-selected bin IDs as a JSON array string.
     * e.g. "[1, 2, 3, 45]"
     */
    @Column(name = "selected_bin_ids", columnDefinition = "TEXT")
    private String selectedBinIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}