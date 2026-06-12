package com.garbo.api.dto;

import lombok.Data;

@Data
public class ComplaintCreateRequest {
    private String title;
    private String issueType;
    private String urgency;
    private String wasteType;
    private String description;
    private String location;
    private String imageUrl;
}
