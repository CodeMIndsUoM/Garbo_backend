package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DraftRouteDTO {
    private String draftId;
    private List<Long> binIds;
    private int binCount;
    private String suggestedZone;
}
