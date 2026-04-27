package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Leaderboard entity for tracking top collectors and their scores.
 * Denormalizes ranking data for efficient queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leaderboards", indexes = {
        @Index(name = "idx_snapshot_date", columnList = "snapshotDate"),
        @Index(name = "idx_rank", columnList = "rank")
})
public class Leaderboard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "collector_id")
    private BinCollector collector;

    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "reward_points", nullable = false)
    private double rewardPoints;
    
    @Column(name = "rank", nullable = false)
    private int rank;  // 1, 2, 3, ...
    
    @Column(name = "collector_name")
    private String collectorName;  // Denormalized for quick display
    
    @Column(name = "collector_email")
    private String collectorEmail;  // Denormalized for queries
    
    @Column(name = "role", nullable = false)
    private String role;  // "COLLECTOR" or "FIELD_MENTOR"
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;  // For daily leaderboard tracking
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    @Version
    @Column(name = "version")
    private Long version;  // For optimistic locking
}
