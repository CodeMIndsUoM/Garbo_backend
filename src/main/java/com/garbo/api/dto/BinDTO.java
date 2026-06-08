package com.garbo.api.dto;

public class BinDTO {

    public Long id;
    public double lat;
    public double lng;
    public int fillLevel;
    public String priority;

    public void setId(Object id2) {
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }

    public void setFillLevel(Object fillLevel2) {
        throw new UnsupportedOperationException("Unimplemented method 'setFillLevel'");
    }

    public void setPriority(Object priority2) {
        throw new UnsupportedOperationException("Unimplemented method 'setPriority'");
    }

    public void setZone(Object zone) {
        throw new UnsupportedOperationException("Unimplemented method 'setZone'");
    }

    public Long getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getFillLevel() {
        return fillLevel;
    }

    public String getPriority() {
        return priority;
    }

    public String getZone() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getZone'");
    }
}
