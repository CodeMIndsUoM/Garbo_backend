package com.garbo.core.entity;

<<<<<<< HEAD
=======

>>>>>>> kevin-RWS
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
<<<<<<< HEAD
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
=======

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
>>>>>>> kevin-RWS
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< HEAD
    @Transient
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String urgency;
    private String wasteType;

    @Column(nullable = false)
    private String location;

    private Double locationLat;
    private Double locationLong;
    private String locationAddress;

    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, RESOLVED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
=======
    @Column(name = "assigned_personnel_id")
    private Long assignedPersonnelId;

    @Column(name = "citizen_id")
    private Long citizenId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "location")
    private String location;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    // Values: "new" | "inprogress" | "completed"
    @Column(name = "status")
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
>>>>>>> kevin-RWS
