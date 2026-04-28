package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class RouteSessionSnapshotDTO {

    public String sessionId;
    public Long userId;
    public long version;
    public String status; // PROCESSING | READY | ERROR | SESSION_CREATED
    public String trigger;

    public List<Long> selectedBinIds;
    public List<Long> addedBinIds;
    public List<Long> removedBinIds;

    public Object route; // RouteResponseDTO or null during processing
    public String message; // for errors (optional)

   
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
                                Instant.now(),
                                trigger,
                                message,
                                selectedBinIds,
                                addedBinIds,
                                removedBinIds,
                                null
                );
        }

        public RouteSessionSnapshotDTO(String sessionId2, Long userId2, long version2, String string, Instant now,
                        String trigger2, String message2, List<Long> selectedBinIds2, List<Long> addedBinIds2,
                        List<Long> removedBinIds2, Object object) {
                //TODO Auto-generated constructor stub
        }
}
