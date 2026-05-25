package com.garbo.api.dto.binAnalyzeDTOs;

public class ZoneAnalyticsDTO {

    private String zone;
    private int total;
    private int empty;
    private int half;
    private int full;
    private int notChecked;
    private int highPriority;
    private int mediumPriority;
    private int lowPriority;

    public ZoneAnalyticsDTO() {}

    public ZoneAnalyticsDTO(String zone, int total,
                             int empty, int half, int full, int notChecked,
                             int highPriority, int mediumPriority, int lowPriority) {
        this.zone         = zone;
        this.total        = total;
        this.empty        = empty;
        this.half         = half;
        this.full         = full;
        this.notChecked   = notChecked;
        this.highPriority = highPriority;
        this.mediumPriority = mediumPriority;
        this.lowPriority  = lowPriority;
    }

    public String getZone()           { return zone; }
    public void   setZone(String z)   { this.zone = z; }

    public int  getTotal()            { return total; }
    public void setTotal(int t)       { this.total = t; }

    public int  getEmpty()            { return empty; }
    public void setEmpty(int e)       { this.empty = e; }

    public int  getHalf()             { return half; }
    public void setHalf(int h)        { this.half = h; }

    public int  getFull()             { return full; }
    public void setFull(int f)        { this.full = f; }

    public int  getNotChecked()       { return notChecked; }
    public void setNotChecked(int n)  { this.notChecked = n; }

    public int  getHighPriority()     { return highPriority; }
    public void setHighPriority(int h){ this.highPriority = h; }

    public int  getMediumPriority()   { return mediumPriority; }
    public void setMediumPriority(int m){ this.mediumPriority = m; }

    public int  getLowPriority()      { return lowPriority; }
    public void setLowPriority(int l) { this.lowPriority = l; }
}