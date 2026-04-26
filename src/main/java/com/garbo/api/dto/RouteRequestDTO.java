package com.garbo.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteRequestDTO {

    private Long userId;

    private int vehicleCount;

    private int[] vehicleCapacities;

    private double depotLat;

    private double depotLng;

    private List<Long> selectedBinIds;
}







