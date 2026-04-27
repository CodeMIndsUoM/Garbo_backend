package com.garbo.core.service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BinChangedEvent {
    private final String changeType;
    private final Long binId;
}