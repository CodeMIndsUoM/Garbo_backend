package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "council_boundaries")
public class CouncilBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String council;

    @Column(name = "depot_lat")
    private Double depotLat;

    @Column(name = "depot_lng")
    private Double depotLng;

    @Column(name = "boundary_points", columnDefinition = "TEXT")
    private String boundaryPoints;
}