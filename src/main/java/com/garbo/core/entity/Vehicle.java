package com.garbo.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "vehicle_code", unique = true, nullable = false)
    private String vehicleCode;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "type")
    private String type;

    @Column(name = "capacity")
    private Double capacity;

    @Column(name = "status")
    private String status = "available";

    @Column(name = "assigned_council")
    private String assignedCouncil;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "fuel_level")
    private Integer fuelLevel = 100;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
