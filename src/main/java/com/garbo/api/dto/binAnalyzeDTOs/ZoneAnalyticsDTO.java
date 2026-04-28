package com.garbo.api.dto.binAnalyzeDTOs;

public class ZoneAnalyticsDTO {

    private String zone;

    private int total;

    private int below30;
    private int fill30_50;
    private int fill50_75;
    private int above75;

    private int highPriority;
    private int mediumPriority;
    private int lowPriority;

    public ZoneAnalyticsDTO() {
    }

    public ZoneAnalyticsDTO(String zone, int total, int below30, int fill30_50, int fill50_75, int above75,
            int highPriority, int mediumPriority, int lowPriority) {
        this.zone = zone;
        this.total = total;
        this.below30 = below30;
        this.fill30_50 = fill30_50;
        this.fill50_75 = fill50_75;
        this.above75 = above75;
        this.highPriority = highPriority;
        this.mediumPriority = mediumPriority;
        this.lowPriority = lowPriority;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getBelow30() {
        return below30;
    }

    public void setBelow30(int below30) {
        this.below30 = below30;
    }

    public int getFill30_50() {
        return fill30_50;
    }

    public void setFill30_50(int fill30_50) {
        this.fill30_50 = fill30_50;
    }

    public int getFill50_75() {
        return fill50_75;
    }

    public void setFill50_75(int fill50_75) {
        this.fill50_75 = fill50_75;
    }

    public int getAbove75() {
        return above75;
    }

    public void setAbove75(int above75) {
        this.above75 = above75;
    }

    public int getHighPriority() {
        return highPriority;
    }

    public void setHighPriority(int highPriority) {
        this.highPriority = highPriority;
    }

    public int getMediumPriority() {
        return mediumPriority;
    }

    public void setMediumPriority(int mediumPriority) {
        this.mediumPriority = mediumPriority;
    }

    public int getLowPriority() {
        return lowPriority;
    }

    public void setLowPriority(int lowPriority) {
        this.lowPriority = lowPriority;
    }
}