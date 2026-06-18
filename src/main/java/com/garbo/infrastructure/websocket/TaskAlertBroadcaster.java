package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.entity.FieldMentor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAlertBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    public void notifyMentorBinAssigned(FieldMentor mentor, Bin bin) {
        if (mentor == null || mentor.getEmpId() == null || bin == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "BIN_ASSIGNED");
        payload.put("binId", bin.getId());
        payload.put("location", bin.getLocation());
        payload.put("council", bin.getCouncil());
        payload.put("binCode", bin.getBinCode());
        payload.put("timestamp", System.currentTimeMillis());

        try {
            messagingTemplate.convertAndSend("/topic/users/" + mentor.getEmpId() + "/tasks", payload);
            sessionManager.sendToUser(
                    mentor.getEmpId(),
                    new WebSocketMessage<>("BIN_ASSIGNED", mentor.getEmpId(), payload));
            log.info("Sent BIN_ASSIGNED alert to mentor empId={} binId={}", mentor.getEmpId(), bin.getId());
        } catch (Exception ex) {
            log.warn("Failed to send mentor task alert: {}", ex.getMessage());
        }
    }

    public void notifyMentorBinSuggestionUpdated(BinSuggestion suggestion) {
        if (suggestion == null || suggestion.getMentorId() == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "BIN_SUGGESTION_UPDATED");
        payload.put("suggestionId", suggestion.getId());
        payload.put("status", suggestion.getStatus());
        payload.put("resolutionNotes", suggestion.getResolutionNotes());
        payload.put("createdBinId", suggestion.getCreatedBinId());
        payload.put("timestamp", System.currentTimeMillis());

        try {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + suggestion.getMentorId() + "/tasks",
                    payload);
            sessionManager.sendToUser(
                    suggestion.getMentorId(),
                    new WebSocketMessage<>("BIN_SUGGESTION_UPDATED", suggestion.getMentorId(), payload));
            log.info(
                    "Sent BIN_SUGGESTION_UPDATED alert to mentor empId={} suggestionId={}",
                    suggestion.getMentorId(),
                    suggestion.getId());
        } catch (Exception ex) {
            log.warn("Failed to send bin suggestion alert: {}", ex.getMessage());
        }
    }

    public void notifyCollectorRouteAssigned(Long collectorEmpId, String sessionId, int binCount, Long vehicleId) {
        if (collectorEmpId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ROUTE_ASSIGNED");
        payload.put("sessionId", sessionId);
        payload.put("binCount", binCount);
        payload.put("vehicleId", vehicleId);
        payload.put("timestamp", System.currentTimeMillis());

        try {
            messagingTemplate.convertAndSend("/topic/routes/users/" + collectorEmpId, payload);
            sessionManager.sendToUser(
                    collectorEmpId,
                    new WebSocketMessage<>("ROUTE_ASSIGNED", collectorEmpId, payload));
            log.info("Sent ROUTE_ASSIGNED alert to collector empId={} sessionId={}", collectorEmpId, sessionId);
        } catch (Exception ex) {
            log.warn("Failed to send collector route alert: {}", ex.getMessage());
        }
    }
}
