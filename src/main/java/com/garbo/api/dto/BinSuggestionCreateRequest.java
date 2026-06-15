package com.garbo.api.dto;

import lombok.Data;

@Data
public class BinSuggestionCreateRequest {
    private String category;
    private String notes;
    private String location;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
}
