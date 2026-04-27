package com.garbo.core.entity;

<<<<<<< HEAD
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
=======
import jakarta.persistence.*;
>>>>>>> kevin-RWS
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

<<<<<<< HEAD
=======
import java.time.LocalDateTime;

>>>>>>> kevin-RWS
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

<<<<<<< HEAD
    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "type")
    private String type;

    @Column(name = "capacity")
    private Double capacity;

    @Column(name = "status")
    private String status = "available";
=======
    @Column(name = "license_plate", unique = true, nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private String type; // Truck, Compactor, Mini Truck

    private Double capacity; // in tons

    @Column(nullable = false)
    private String status = "available"; // available, on_route, maintenance, inactive
>>>>>>> kevin-RWS

    @Column(name = "assigned_council")
    private String assignedCouncil;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "fuel_level")
<<<<<<< HEAD
    private Integer fuelLevel = 100;
=======
    private Integer fuelLevel = 100; // 0-100%
>>>>>>> kevin-RWS

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;

<<<<<<< HEAD
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
=======
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
>>>>>>> kevin-RWS
