package com.garbo.core.entity;

import com.garbo.api.dto.RouteVehicleRouteDTO;
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

    @Column(name = "session_id", nullable = false)
    private java.util.UUID sessionId;

    @Column(name = "vehicle_key", nullable = false, length = 50)
    private String vehicleKey;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "total_bins")
    private Integer totalBins;

    @Column(name = "estimated_duration_seconds")
    private Double estimatedDurationSeconds;

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

    public RouteVehicleRouteDTO toDTO() {
        RouteVehicleRouteDTO dto = new RouteVehicleRouteDTO();
        dto.setId(this.id);
        dto.setSessionId(this.sessionId);
        dto.setVehicleKey(this.vehicleKey);
        dto.setCapacity(this.capacity);
        dto.setTotalBins(this.totalBins);
        dto.setEstimatedDurationSeconds(this.estimatedDurationSeconds);
        dto.setBinStops(
            this.binStops.stream()
                .map(RouteBinStop::toDTO)
                .toList()
        );
        return dto;
    }
}