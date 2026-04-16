package com.garbo.core.service.route;

import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class RouteSessionState {

    private final String sessionId;
    private final Long userId;
    private final RouteSessionCreateRequestDTO config;

    private final AtomicLong version = new AtomicLong(0);

    private volatile long generation;

    private volatile RouteSessionSnapshotDTO latest;

    private volatile ScheduledFuture<?> scheduledFuture;

    private volatile CompletableFuture<?> runningFuture;

    // FIX: avoid shared mutable list issues
    private volatile List<Long> activeBinIds = new ArrayList<>();

    public RouteSessionState(String sessionId, Long userId, RouteSessionCreateRequestDTO config) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.config = config;
    }

    public synchronized long nextGeneration() {
        generation++;
        return generation;
    }
} 
 
 
 
 /*package com.garbo.core.service.route;

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
    private final String sessionId;
    private final Long userId;
    private final RouteSessionCreateRequestDTO config;
    private final AtomicLong version = new AtomicLong(0);
    private volatile long generation;
    private volatile RouteSessionSnapshotDTO latest;
    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile CompletableFuture<?> runningFuture;
    private volatile List<Long> activeBinIds = new ArrayList<>();

    public RouteSessionState(String sessionId, Long userId, RouteSessionCreateRequestDTO config) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.config = config;
    }

    public long nextGeneration() {
        generation++;
        return generation;
    }
}

*/