package com.garbo.core.service.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BinChangedEvent {
    private final String changeType;
    private final Long binId;
    private final String status;
    private final Integer fillLevel;
    private final LocalDateTime lastChecked;
    private final Long reportId;
    private final String notes;
    private final String photoUrl;
    private final String reporterName;

    public BinChangedEvent(String changeType, Long binId) {
        this(changeType, binId, null, null, null, null, null, null, null);
    }

    public BinChangedEvent(String changeType, Object id) {
        this(changeType, parseLong(id), null, null, null, null, null, null, null);
    }

    public BinChangedEvent(
            String changeType,
            Long binId,
            String status,
            Integer fillLevel,
            LocalDateTime lastChecked
    ) {
        this(changeType, binId, status, fillLevel, lastChecked, null, null, null, null);
    }

    public BinChangedEvent(
            String changeType,
            Long binId,
            String status,
            Integer fillLevel,
            LocalDateTime lastChecked,
            Long reportId,
            String notes,
            String photoUrl,
            String reporterName
    ) {
        this.changeType = changeType;
        this.binId = binId;
        this.status = status;
        this.fillLevel = fillLevel;
        this.lastChecked = lastChecked;
        this.reportId = reportId;
        this.notes = notes;
        this.photoUrl = photoUrl;
        this.reporterName = reporterName;
    }

    private static Long parseLong(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Long longId) {
            return longId;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(id.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
