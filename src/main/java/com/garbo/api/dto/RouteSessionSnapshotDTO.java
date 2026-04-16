package com.garbo.api.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RouteSessionSnapshotDTO {
    public String sessionId;
    public Long userId;
    public long version;
    public String status;
    public Instant generatedAt;
    public String trigger;
    public String message;
    public List<Long> selectedBinIds;
    public List<Long> addedBinIds;
    public List<Long> removedBinIds;
    public RouteResponseDTO route;

        public static RouteSessionSnapshotDTO processing(
                        String sessionId,
                        Long userId,
                        long version,
                        String trigger,
                        List<Long> selectedBinIds,
                        List<Long> addedBinIds,
                        List<Long> removedBinIds
        ) {
        return new RouteSessionSnapshotDTO(
                sessionId,
                userId,
                version,
                "PROCESSING",
                Instant.now(),
                trigger,
                "Route recomputation started",
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                null
        );
    }

    public static RouteSessionSnapshotDTO ready(
            String sessionId,
            Long userId,
            long version,
            String trigger,
            List<Long> selectedBinIds,
            List<Long> addedBinIds,
            List<Long> removedBinIds,
            RouteResponseDTO route
    ) {
        return new RouteSessionSnapshotDTO(
                sessionId,
                userId,
                version,
                "READY",
                Instant.now(),
                trigger,
                null,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                route
        );
    }

    public static RouteSessionSnapshotDTO error(
            String sessionId,
            Long userId,
            long version,
            String trigger,
            List<Long> selectedBinIds,
            List<Long> addedBinIds,
            List<Long> removedBinIds,
            String message
    ) {
        return new RouteSessionSnapshotDTO(
                sessionId,
                userId,
                version,
                "ERROR",
                Instant.now(),
                trigger,
                message,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                null
        );
    }
}
