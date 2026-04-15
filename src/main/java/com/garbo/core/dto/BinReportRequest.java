package com.garbo.core.dto;

import lombok.Data;

@Data
public class BinReportRequest {
    private Integer fillLevel;
    private String status;
    private String notes;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
}
