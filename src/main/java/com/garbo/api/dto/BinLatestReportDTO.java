package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinLatestReportDTO {
    private Long reportId;
    private Long binId;
    private String binCode;
    private String council;
    private String status;
    private Integer fillLevel;
    private String notes;
    private String photoUrl;
    private String reporterName;
    private LocalDateTime reportedAt;
    private Boolean discrepancy;
    private String previousStatus;
}
