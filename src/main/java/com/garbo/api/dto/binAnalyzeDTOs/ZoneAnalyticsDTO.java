package com.garbo.api.dto.binAnalyzeDTOs;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ZoneAnalyticsDTO {

    private String zone;

    private int total;

    private int below30;
    private int fill30_50;
    private int fill50_75;
    private int above75;

    private int highPriority;
    private int mediumPriority;
    private int lowPriority;
}