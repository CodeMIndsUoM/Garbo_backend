package com.garbo.api.dto;

import lombok.Data;

@Data
public class BinDTO {
    private Long id;
    private double lat;
    private double lng;
    private int fillLevel;
    private String priority;
    private String zone;
}