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

    public BinChangedEvent(String changeType, Long binId) {
        this(changeType, binId, null, null, null);
    }

    public BinChangedEvent(String changeType, Object id) {
        this(changeType, parseLong(id), null, null, null);
    }

    public BinChangedEvent(
            String changeType,
            Long binId,
            String status,
            Integer fillLevel,
            LocalDateTime lastChecked
    ) {
        this.changeType = changeType;
        this.binId = binId;
        this.status = status;
        this.fillLevel = fillLevel;
        this.lastChecked = lastChecked;
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
