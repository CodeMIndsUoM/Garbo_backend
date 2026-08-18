package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assigned_personnel_id")
    private Long assignedPersonnelId;

    @Column(name = "citizen_id")
    private Long citizenId;

    @Column(name = "title")
    private String title;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "urgency")
    private String urgency;

    @Column(name = "waste_type")
    private String wasteType;

    @Column(name = "council")
    private String council;

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

    @Column(name = "status")
    private String status;

    @Column(name = "is_confirmed_true")
    private Boolean isConfirmedTrue;

    @Column(name = "field_staff_note", columnDefinition = "TEXT")
    private String fieldStaffNote;

    @Column(name = "field_staff_photo_url")
    private String fieldStaffPhotoUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
