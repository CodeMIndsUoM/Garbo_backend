package com.garbo.api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RouteBinStopDTO {
    private Long id;
    private Integer stopOrder;
    private Long binId;
    private Double lat;
    private Double lng;
    private Double durationFromPrevSeconds;
    private String status;
    private LocalDateTime collectedAt;
}