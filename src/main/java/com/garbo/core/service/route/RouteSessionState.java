package com.garbo.core.service.route;

import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class RouteSessionState {
    private final java.util.UUID sessionId;
    private final Long userId;
    private volatile String workShift;
    private RouteSessionCreateRequestDTO config;
    private final AtomicLong version = new AtomicLong(0);
    private volatile long generation;
    private volatile RouteSessionSnapshotDTO latest;
    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile CompletableFuture<?> runningFuture;
    private volatile List<Long> activeBinIds = new ArrayList<>();

    public RouteSessionState(java.util.UUID sessionId, Long userId, RouteSessionCreateRequestDTO config) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.config = config;
    }

    public long nextGeneration() {
        generation++;
        return generation;
    }
}
