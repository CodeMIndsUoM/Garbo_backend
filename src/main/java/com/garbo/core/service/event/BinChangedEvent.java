package com.garbo.core.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BinChangedEvent {
    private String changeType;
    private Long binId;
}
