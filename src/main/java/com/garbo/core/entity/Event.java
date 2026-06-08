package com.garbo.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String category;
    private String imageUrl;
    private Integer maxParticipants;
    private Integer enrolledCount = 0;
    private String status = "ACTIVE";
    private String rejectionReason;

    @Column(nullable = false)
    private String council;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_citizen_id", nullable = false)
    private User organizerCitizen;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enrolledCount == null) {
            enrolledCount = 0;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
