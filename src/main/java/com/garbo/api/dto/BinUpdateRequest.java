package com.garbo.api.dto;

import lombok.Data;

@Data
public class BinUpdateRequest {
    private String location;
    private String category;
    private String binCode;
    private Double latitude;
    private Double longitude;
    private String coordinates;
    private String status;
    private String priority;
}
