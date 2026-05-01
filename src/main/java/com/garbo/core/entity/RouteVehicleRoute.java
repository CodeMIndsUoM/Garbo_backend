package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "route_vehicle_routes")
public class RouteVehicleRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References route_sessions.session_id.
     */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    /**
     * The key from RouteResponseDTO.routes Map — e.g. "0", "1", "2".
     * Kept as a String so it exactly matches the DTO structure for easy mapping.
     */
    @Column(name = "vehicle_key", nullable = false, length = 50)
    private String vehicleKey;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "total_bins")
    private Integer totalBins;

    @Column(name = "estimated_duration_seconds")
    private Double estimatedDurationSeconds;

    /**
     * Ordered list of bin stops for this vehicle route.
     * orphanRemoval = true so stops are deleted if this route is removed.
     */
    @OneToMany(
        mappedBy = "vehicleRoute",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("stopOrder ASC")
    private List<RouteBinStop> binStops = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}