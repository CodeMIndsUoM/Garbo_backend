package com.garbo.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "type")
    private String type;

    @Column(name = "capacity")
    private Double capacity;

    /** Maximum bins this vehicle can collect in one route trip. */
    @Column(name = "max_bins")
    private Integer maxBins;

    @Column(name = "status")
    private String status = "available";

    @Column(name = "assigned_council")
    private String assignedCouncil;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @jakarta.persistence.Transient
    private String assignedDriverName;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
