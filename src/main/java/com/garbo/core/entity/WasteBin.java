package com.garbo.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bins")
public class WasteBin {

    @Id
    @Column(name = "id")
    private String id;

    @Transient
    private String binCode;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "type")
    private String type;

    @Column(name = "fill_level")
    private Integer fillLevel = 0;

    @Column(name = "status")
    private String status = "normal";

    @Column(name = "last_collection")
    private String lastCollection;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "council")
    private String council;

    @Column(name = "coordinates")
    private String coordinates;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
