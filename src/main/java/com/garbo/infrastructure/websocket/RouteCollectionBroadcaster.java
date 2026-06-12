package com.garbo.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCollectionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final CouncilBinStompBroadcaster councilBinStompBroadcaster;

    public void broadcastBinStatusUpdate(UUID sessionId, Long binId, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId.toString());
        payload.put("binId", binId);
        payload.put("status", status);
        payload.put("timestamp", System.currentTimeMillis());

        try {
            messagingTemplate.convertAndSend("/topic/route-sessions/" + sessionId + "/bins", payload);
            messagingTemplate.convertAndSend("/topic/route-collection", payload);
            councilBinStompBroadcaster.publishCollectionUpdate(
                    binId,
                    status,
                    sessionId != null ? sessionId.toString() : null);
            log.debug("Broadcast bin collection update session={} bin={} status={}", sessionId, binId, status);
        } catch (Exception e) {
            log.warn("Failed to broadcast bin collection update: {}", e.getMessage());
        }
    }
}
