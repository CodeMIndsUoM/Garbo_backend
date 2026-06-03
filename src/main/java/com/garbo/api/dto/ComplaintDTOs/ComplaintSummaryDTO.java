package com.garbo.api.dto.ComplaintDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryDTO {
    private long   pendingCount;
    private long   acceptedCount;
    private double resolutionRate;  // acceptedCount / total * 100
}