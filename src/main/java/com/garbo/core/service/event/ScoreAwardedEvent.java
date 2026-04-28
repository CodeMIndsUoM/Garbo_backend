package com.garbo.core.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAwardedEvent {
    private Long userId;
    private String role;
    private Long taskId;
    private String sourceEventId;
}
