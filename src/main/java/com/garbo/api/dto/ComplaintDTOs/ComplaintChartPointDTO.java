package com.garbo.api.dto.ComplaintDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One point on the area chart — a time bucket with 3 status counts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintChartPointDTO {
    private String label;        // "Mar 01", "Mon", "Today" etc.
    private long   newCount;
    private long   inProgress;
    private long   resolved;
}