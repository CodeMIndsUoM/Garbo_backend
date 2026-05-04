package com.garbo.core.service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BinChangedEvent {
    public BinChangedEvent(String changeType, Object id) {
        this.changeType = changeType;
        this.binId = null; // or (Long) id if appropriate
    }
    private final String changeType;
    private final Long binId;
}