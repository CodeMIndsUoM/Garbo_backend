package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.BinStatusUpdatedPayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Converts bin-status domain events into websocket pushes for dashboards.
 */
@Slf4j
@Component
public class BinStatusBroadcaster {

    private static final Set<String> BIN_STATUS_PUSH_TYPES = Set.of(
            "STATUS_REPORTED",
            "STATUS_UNDONE",
            "COLLECTED"
    );

    private final BinRepository binRepository;
    private final WebSocketSessionManager sessionManager;
    private final CouncilBinStompBroadcaster councilBinStompBroadcaster;

    public BinStatusBroadcaster(
            BinRepository binRepository,
            WebSocketSessionManager sessionManager,
            CouncilBinStompBroadcaster councilBinStompBroadcaster) {
        this.binRepository = binRepository;
        this.sessionManager = sessionManager;
        this.councilBinStompBroadcaster = councilBinStompBroadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBinChanged(BinChangedEvent event) {
        if (event == null || event.getBinId() == null) {
            return;
        }

        String changeType = event.getChangeType() == null ? "" : event.getChangeType().toUpperCase();
        if (!BIN_STATUS_PUSH_TYPES.contains(changeType)) {
            return;
        }

        broadcastBinStatus(event, changeType);
    }

    private void broadcastBinStatus(BinChangedEvent event, String changeType) {
        binRepository.findByNumericId(event.getBinId()).ifPresentOrElse(bin -> {
            BinStatusUpdatedPayload payload = buildPayload(bin, event, changeType);
            WebSocketMessage<BinStatusUpdatedPayload> message = new WebSocketMessage<>(
                    "BIN_STATUS_UPDATED",
                    payload.getAssignedToEmpId(),
                    payload
            );

            sessionManager.broadcastToAll(message);
            councilBinStompBroadcaster.publishStatusFromEvent(event);

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
