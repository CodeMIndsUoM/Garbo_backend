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
@Table(name = "bins")
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bin_code", unique = true, nullable = false)
    private String binCode;

    private String location;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private String type;

    @Column(name = "fill_level")
    private Integer fillLevel = 0;

    @Column(name = "battery_level")
    private Integer batteryLevel = 100;

    @Column(nullable = false)
    private String status = "active";

    private String council;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_collection_at")
    private LocalDateTime lastCollectionAt;

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
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bins")
public class Bin {
    @Id
    @Column(length = 20)
    private String id;

    private String location;

    @Column(name = "type", length = 20)
    private String category; // public/park/commercial/medical/education

    @Column(length = 20)
    private String status = "notChecked"; // notChecked/full/half/empty

    @Column(name = "fill_level")
    private Integer fillLevel;

    @Column(length = 50)
    private String zone;

    @Column(name = "lat")
    private Double latitude;

    @Column(name = "lng")
    private Double longitude;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    private String priority;

    @ManyToOne
    @JoinColumn(name = "assigned_to", referencedColumnName = "emp_id", nullable = true)
    private FieldMentor assignedTo;
}
