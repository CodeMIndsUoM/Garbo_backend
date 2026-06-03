package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.BinStatusUpdatedPayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Converts bin-status domain events into websocket pushes for dashboards.
 *
 * We only push report/undo style updates because those are the events that
 * field-staff dashboard needs for near real-time stat tiles and bin list refresh.
 */
@Slf4j
@Component
public class BinStatusBroadcaster {

    private static final Set<String> BIN_STATUS_PUSH_TYPES = Set.of(
            "STATUS_REPORTED",
            "STATUS_UNDONE"
    );

    private final BinRepository binRepository;
    private final WebSocketSessionManager sessionManager;

    public BinStatusBroadcaster(BinRepository binRepository, WebSocketSessionManager sessionManager) {
        this.binRepository = binRepository;
        this.sessionManager = sessionManager;
    }

    @EventListener
    public void onBinChanged(BinChangedEvent event) {
        if (event == null || event.getBinId() == null) {
            return;
        }

        String changeType = event.getChangeType() == null ? "" : event.getChangeType().toUpperCase();
        if (!BIN_STATUS_PUSH_TYPES.contains(changeType)) {
            return;
        }

        CompletableFuture.runAsync(() -> broadcastBinStatus(event, changeType));
    }

    private void broadcastBinStatus(BinChangedEvent event, String changeType) {
        binRepository.findByNumericId(event.getBinId()).ifPresentOrElse(bin -> {
            BinStatusUpdatedPayload payload = buildPayload(bin, event, changeType);
            WebSocketMessage<BinStatusUpdatedPayload> message = new WebSocketMessage<>(
                    "BIN_STATUS_UPDATED",
                    payload.getAssignedToEmpId(),
                    payload
            );

            // Bin status is operational dashboard data, so broadcast it to all
            // authenticated realtime clients. Field-staff clients filter by
            // assignedToEmpId; admin dashboards need the same event for live
            // monitoring without a manual refresh.
            sessionManager.broadcastToAll(message);

            log.info("Broadcast BIN_STATUS_UPDATED for binId={}, type={}", payload.getBinId(), changeType);
        }, () -> log.warn("Skipping BIN_STATUS_UPDATED broadcast; bin not found for id={}", event.getBinId()));
    }

    private BinStatusUpdatedPayload buildPayload(Bin bin, BinChangedEvent event, String changeType) {
        Long assignedEmpId = null;
        if (bin.getAssignedTo() != null) {
            assignedEmpId = bin.getAssignedTo().getEmpId();
        }

        LocalDateTime lastChecked = event.getLastChecked() != null
                ? event.getLastChecked()
                : bin.getLastChecked();
        String lastCheckedIso = null;
        if (lastChecked != null) {
            lastCheckedIso = lastChecked.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        return new BinStatusUpdatedPayload(
                bin.getId(),
                event.getStatus() != null ? event.getStatus() : bin.getStatus(),
                event.getFillLevel() != null ? event.getFillLevel() : bin.getFillLevel(),
                lastCheckedIso,
                assignedEmpId,
                changeType,
                System.currentTimeMillis()
        );
    }
}
