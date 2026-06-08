package com.garbo.api.dto.staffAnalyzeDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAnalyticsResponseDTO {
    private StaffSummaryDTO    summary;
    private List<ZoneStaffDTO> zoneData;
}