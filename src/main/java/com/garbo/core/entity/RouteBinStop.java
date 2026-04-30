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
@Table(name = "route_bin_stops")
public class RouteBinStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent vehicle route — bidirectional link to RouteVehicleRoute.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_route_id", nullable = false)
    private RouteVehicleRoute vehicleRoute;

    /**
     * 1-based position in the collection sequence for this vehicle.
     */
    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    /**
     * References bins.id — no @ManyToOne to avoid pulling in the full Bin graph
     * on every stop query. Fetch the bin separately when needed.
     */
    @Column(name = "bin_id", nullable = false)
    private Long binId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    /**
     * Travel time in seconds from the previous stop (or depot for stop 1).
     * Sourced from the OSRM duration matrix.
     */
    @Column(name = "duration_from_prev_seconds")
    private Double durationFromPrevSeconds;

    /**
     * Real-time collection status.
     * PENDING  — not yet visited
     * COLLECTED — bin was emptied by the collector team
     * SKIPPED  — bin was bypassed (e.g. already empty, blocked access)
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * Timestamp set when status transitions to COLLECTED.
     * Updated by the collector via WebSocket BIN_COLLECTED message.
     */
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}