package com.garbo.api.dto.Collect_analyze_dtos;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardResponseDTO {
    private int assigned;
    private int collected;
    private int missed;
    private List<ChartDataDTO> chartData;
}