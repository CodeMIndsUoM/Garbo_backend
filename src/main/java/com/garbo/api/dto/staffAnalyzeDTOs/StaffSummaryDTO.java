package com.garbo.api.dto.staffAnalyzeDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDTO {
    private long   totalStaff;
    private long   onDutyCount;
    private long   onLeaveCount;
    private double attendanceRate;
    private double avgPerformance;
}