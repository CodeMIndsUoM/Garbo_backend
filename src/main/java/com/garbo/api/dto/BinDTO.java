package com.garbo.api.dto;

import lombok.Data;

@Data
public class BinDTO {
    public double lat;
    public double lng;
    public int fillLevel;
    public String priority;
}
