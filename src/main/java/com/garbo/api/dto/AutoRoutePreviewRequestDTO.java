package com.garbo.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AutoRoutePreviewRequestDTO {
    private String council;
    private List<String> minFillStatus;
    private boolean useZones = true;
}
