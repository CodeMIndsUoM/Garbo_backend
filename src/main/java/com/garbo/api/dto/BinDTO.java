package com.garbo.api.dto;

<<<<<<< HEAD
public class BinDTO {

    public Long id;
    public double lat;
    public double lng;
    public int fillLevel;
    public String priority;
}
=======
import lombok.Data;

@Data
public class BinDTO {
    private Long id;
    private double lat;
    private double lng;
    private int fillLevel;
    private String priority;
    private String zone;
}
>>>>>>> kevin-RWS
