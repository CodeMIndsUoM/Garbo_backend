package com.garbo.core.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bins")
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double lat;
    private double lng;
    private int fillLevel;
    private String priority;

    public Bin() {}

    public Bin(double lat, double lng, int fillLevel, String priority) {
        this.lat = lat;
        this.lng = lng;
        this.fillLevel = fillLevel;
        this.priority = priority;
    }

    // Getters
    public Long getId() { return id; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public int getFillLevel() { return fillLevel; }
    public String getPriority() { return priority; }

    // Setters (NO setId!)
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
    public void setFillLevel(int fillLevel) { this.fillLevel = fillLevel; }
    public void setPriority(String priority) { this.priority = priority; }
}