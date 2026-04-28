package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bins")
public class Bin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // Compatibility getters for team's Route Optimization code
    public Double getLat() {
        return latitude;
    }

    public Double getLng() {
        return longitude;
    }

    // Constructor for team's BinMapper/seeding code if needed
    public Bin(double latitude, double longitude, int fillLevel, String status) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.fillLevel = fillLevel;
        this.status = status;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFillLevel(Integer fillLevel) {
        this.fillLevel = fillLevel;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setAssignedTo(FieldMentor assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setLng(double lng) {
        this.longitude = lng;
    }
}
