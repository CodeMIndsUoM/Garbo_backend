package com.garbo.core.entity;

import jakarta.persistence.*;
<<<<<<< HEAD
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
=======
import lombok.Data;

@Entity
@Table(name = "bins")
@Data
public class Bin {

>>>>>>> kevin-RWS
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< HEAD
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
}
=======
    private double lat;
    private double lng;

    private int fillLevel;

    private String priority;

    private String zone;
}
>>>>>>> kevin-RWS
