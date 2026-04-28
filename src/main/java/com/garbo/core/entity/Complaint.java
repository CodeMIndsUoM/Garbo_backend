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

    public void setCitizen(User citizen) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCitizen'");
    }

    public Object getCategory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCategory'");
    }

    public Object getTitle() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTitle'");
    }

    public void setCategory(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCategory'");
    }

    public String getLocationAddress() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocationAddress'");
    }

    public User getCitizen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCitizen'");
    }

    public void setAssignedTo(User personnel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAssignedTo'");
    }

    public void setTitle(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTitle'");
    }
}
