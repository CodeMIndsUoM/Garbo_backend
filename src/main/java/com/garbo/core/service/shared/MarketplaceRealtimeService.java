package com.garbo.core.service.shared;

import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.entity.CollectionRequest;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class MarketplaceRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ThirdPartyCollectorRepository collectorRepository;

    public MarketplaceRealtimeService(
            SimpMessagingTemplate messagingTemplate,
            ThirdPartyCollectorRepository collectorRepository) {
        this.messagingTemplate = messagingTemplate;
        this.collectorRepository = collectorRepository;
    }

    public void publishRequestCreated(CollectionRequest request) {
        if (request == null || request.getId() == null) {
            return;
        }
        Long citizenId = request.getCitizen() != null ? request.getCitizen().getEmpId() : null;
        if (citizenId != null) {
            publishToUser(citizenId, "REQUEST_UPDATED", request.getId(), null, request.getStatus().name());
        }
        notifyCollectorsInCouncil(request.getCouncil(), request.getId(), null, request.getStatus().name());
    }

    public void publishRequestChanged(CollectionRequest request) {
        if (request == null || request.getId() == null) {
            return;
        }
        Long citizenId = request.getCitizen() != null ? request.getCitizen().getEmpId() : null;
        if (citizenId != null) {
            publishToUser(citizenId, "REQUEST_UPDATED", request.getId(), null, request.getStatus().name());
        }
    }

    public void publishOfferChanged(CollectionOffer offer) {
        if (offer == null || offer.getId() == null || offer.getRequest() == null) {
            return;
        }
        CollectionRequest request = offer.getRequest();
        Long requestId = request.getId();
        Long offerId = offer.getId();
        String status = offer.getStatus() != null ? offer.getStatus().name() : "";

        Long citizenId = request.getCitizen() != null ? request.getCitizen().getEmpId() : null;
        if (citizenId != null) {
            publishToUser(citizenId, "OFFER_UPDATED", requestId, offerId, status);
            publishToUser(citizenId, "REQUEST_UPDATED", requestId, offerId, request.getStatus().name());
        }

        Long collectorId = offer.getCollector() != null ? offer.getCollector().getEmpId() : null;
        if (collectorId != null) {
            publishToUser(collectorId, "OFFER_UPDATED", requestId, offerId, status);
        }
    }

    private void notifyCollectorsInCouncil(String council, Long requestId, Long offerId, String status) {
        if (council == null || council.isBlank()) {
            return;
        }
        List<ThirdPartyCollector> collectors = collectorRepository.findAll();
        for (ThirdPartyCollector collector : collectors) {
            if (collector.getRegistrationStatus() != RegistrationStatus.APPROVED) {
                continue;
            }
            if (!isAssignedToCouncil(collector.getAssignedCouncils(), council)) {
                continue;
            }
            publishToUser(collector.getEmpId(), "REQUEST_UPDATED", requestId, offerId, status);
        }
    }

    private boolean isAssignedToCouncil(String assignedCouncils, String council) {
        if (assignedCouncils == null || assignedCouncils.isBlank()) {
            return false;
        }
        String normalizedCouncil = council.trim().toLowerCase(Locale.ROOT);
        for (String part : assignedCouncils.split(",")) {
            if (part.trim().equalsIgnoreCase(normalizedCouncil)
                    || part.trim().equalsIgnoreCase(council.trim())) {
                return true;
            }
        }
        return false;
    }

    private void publishToUser(
            Long userId,
            String type,
            Long requestId,
            Long offerId,
            String status) {
        if (userId == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        if (offerId != null) {
            payload.put("offerId", offerId);
        }
        payload.put("status", status);

        WebSocketMessage<Map<String, Object>> message =
                new WebSocketMessage<>(type, userId, payload);
        String destination = "/topic/users/" + userId + "/marketplace";
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception ex) {
            log.warn("Failed to publish marketplace event to {}: {}", destination, ex.getMessage());
        }
    }
}
