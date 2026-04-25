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
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_code", unique = true, nullable = false)
    private String vehicleCode;

    @Column(name = "license_plate", unique = true, nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private String type; // Truck, Compactor, Mini Truck

    private Double capacity; // in tons

    @Column(nullable = false)
    private String status = "available"; // available, on_route, maintenance, inactive

    @Column(name = "assigned_council")
    private String assignedCouncil;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "fuel_level")
    private Integer fuelLevel = 100; // 0-100%

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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