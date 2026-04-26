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
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
