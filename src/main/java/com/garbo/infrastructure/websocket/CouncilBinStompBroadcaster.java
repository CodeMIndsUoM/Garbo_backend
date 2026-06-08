package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.CouncilBinUpdateMessage;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouncilBinStompBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final BinRepository binRepository;

    public void publishStatusFromEvent(BinChangedEvent event) {
        if (event == null || event.getBinId() == null) {
            return;
        }
        binRepository.findByNumericId(event.getBinId()).ifPresentOrElse(bin -> {
            String status = event.getStatus() != null ? event.getStatus() : bin.getStatus();
            Integer fillLevel = event.getFillLevel() != null ? event.getFillLevel() : bin.getFillLevel();
            String reportedAt = event.getLastChecked() != null
                    ? event.getLastChecked().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : null;

            CouncilBinUpdateMessage message = CouncilBinUpdateMessage.builder()
                    .type("BIN_STATUS_UPDATED")
                    .binId(bin.getId())
                    .status(status)
                    .fillLevel(fillLevel)
                    .council(bin.getCouncil())
                    .changeType(event.getChangeType())
                    .reportId(event.getReportId())
                    .notes(event.getNotes())
                    .photoUrl(event.getPhotoUrl())
                    .reporterName(event.getReporterName())
                    .reportedAt(reportedAt)
                    .timestamp(System.currentTimeMillis())
                    .build();
            send(message);
        }, () -> log.warn("Skipping council bin STOMP update; bin not found for id={}", event.getBinId()));
    }

    public void publishReportPhotoUpdate(Long binId, Long reportId, String photoUrl) {
        if (binId == null || reportId == null || photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        binRepository.findByNumericId(binId).ifPresent(bin -> {
            CouncilBinUpdateMessage message = CouncilBinUpdateMessage.builder()
                    .type("BIN_STATUS_UPDATED")
                    .binId(binId)
                    .status(bin.getStatus())
                    .fillLevel(bin.getFillLevel())
                    .council(bin.getCouncil())
                    .changeType("REPORT_PHOTO_ATTACHED")
                    .reportId(reportId)
                    .photoUrl(photoUrl)
                    .timestamp(System.currentTimeMillis())
                    .build();
            send(message);
        });
    }

    public void publishCollectionUpdate(Long binId, String collectionStatus, String sessionId) {
        if (binId == null) {
            return;
        }
        Optional<Bin> binOpt = binRepository.findByNumericId(binId);
        String council = binOpt.map(Bin::getCouncil).orElse(null);
        String binStatus = binOpt.map(Bin::getStatus).orElse(null);
        Integer fillLevel = binOpt.map(Bin::getFillLevel).orElse(null);

        CouncilBinUpdateMessage message = CouncilBinUpdateMessage.builder()
                .type("BIN_COLLECTED")
                .binId(binId)
                .status(binStatus)
                .fillLevel(fillLevel)
                .council(council)
                .collectionStatus(collectionStatus)
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .build();
        send(message);
    }

    private void send(CouncilBinUpdateMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/councils/all/bins", message);
            String councilKey = councilTopicKey(message.getCouncil());
            if (councilKey != null) {
                messagingTemplate.convertAndSend("/topic/councils/" + councilKey + "/bins", message);
            }
            log.debug("STOMP bin update type={} binId={} council={}",
                    message.getType(), message.getBinId(), message.getCouncil());
        } catch (Exception ex) {
            log.warn("Failed to publish council bin STOMP update: {}", ex.getMessage());
        }
    }

    public static String councilTopicKey(String council) {
        if (council == null || council.isBlank()) {
            return null;
        }
        return council.trim().replace(" ", "_");
    }
}
