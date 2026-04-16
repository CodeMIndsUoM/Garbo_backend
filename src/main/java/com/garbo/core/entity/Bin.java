package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "bins")
@Data
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double lat;
    private double lng;

    private int fillLevel;

    private String priority;
}