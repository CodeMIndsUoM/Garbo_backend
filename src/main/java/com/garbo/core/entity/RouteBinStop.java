package com.garbo.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garbo.api.dto.RouteBinStopDTO;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_route_id", nullable = false)
    private RouteVehicleRoute vehicleRoute;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "bin_id", nullable = false)
    private Long binId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "duration_from_prev_seconds")
    private Double durationFromPrevSeconds;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

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

    public RouteBinStopDTO toDTO() {
        RouteBinStopDTO dto = new RouteBinStopDTO();
        dto.setId(this.id);
        dto.setStopOrder(this.stopOrder);
        dto.setBinId(this.binId);
        dto.setLat(this.lat);
        dto.setLng(this.lng);
        dto.setDurationFromPrevSeconds(this.durationFromPrevSeconds);
        dto.setStatus(this.status);
        dto.setCollectedAt(this.collectedAt);
        return dto;
    }
}