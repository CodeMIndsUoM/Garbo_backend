package com.garbo.api.dto.Collect_analyze_dtos;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartDataDTO {
    private String time;
    private int assigned;
    private int collected;
    private int missed;
}