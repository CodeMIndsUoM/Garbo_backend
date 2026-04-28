package com.garbo.core.service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BinChangedEvent {
    public BinChangedEvent(String changeType2, Object id) {
        this.changeType = "";
        this.binId = null;
        //TODO Auto-generated constructor stub
    }
    private final String changeType;
    private final Long binId;
}