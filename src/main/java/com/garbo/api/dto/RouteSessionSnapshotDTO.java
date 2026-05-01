package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RouteSessionSnapshotDTO {

    public String sessionId;
    public Long userId;
    public long version;
    public String status; // PROCESSING | READY | ERROR | WARNING | SESSION_CREATED
    public String trigger;

    public List<Long> selectedBinIds;
    public List<Long> addedBinIds;
    public List<Long> removedBinIds;

    public Object route; // RouteResponseDTO or null during processing
    public String message; // error or warning message (optional)

    // ─────────────────────────────────────────────────────────────────────────
    // FACTORY METHODS
    // ─────────────────────────────────────────────────────────────────────────

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
                trigger,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                null,
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
            Object route
    ) {
        return new RouteSessionSnapshotDTO(
                sessionId,
                userId,
                version,
                "READY",
                trigger,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                route,
                null
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
                trigger,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                null,
                message
        );
    }

    /**
     * Fixed: previously passed Instant.now() as a 5th argument which had no
     * matching constructor. Now uses the standard 10-arg constructor directly.
     */
    public static RouteSessionSnapshotDTO warning(
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
                "WARNING",
                trigger,
                selectedBinIds,
                addedBinIds,
                removedBinIds,
                null,
                message
        );
    }
}