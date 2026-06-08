package com.garbo.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.api.dto.websocket.*;
import com.garbo.core.service.BinCollectionRealtimeService;
import com.garbo.core.service.route.RouteSessionService;
import com.garbo.infrastructure.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for GARBO real-time communication.
 * Manages authentication handshaking and routes messages to broadcasters.
 */
@Slf4j
@Component
public class GarboWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private RouteSessionService routeSessionService;

    @Autowired
    private BinCollectionRealtimeService binCollectionRealtimeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Track sessions that are awaiting auth (not yet confirmed)
    private final ConcurrentHashMap<String, WebSocketSession> pendingAuthSessions = new ConcurrentHashMap<>();
    
    /**
     * Called when a WebSocket connection is established.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        pendingAuthSessions.put(sessionId, session);
        log.info("New WebSocket connection established: {}", sessionId);
    }
    
    /**
     * Called when a message is received from the client.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        log.debug("WebSocket message received on session {}: {}", sessionId, payload);
        
        try {
            WebSocketMessage<?> wsMsg = objectMapper.readValue(payload, WebSocketMessage.class);
            
            // Handle AUTH handshake
            if ("AUTH".equalsIgnoreCase(wsMsg.getType())) {
                handleAuthHandshake(session, wsMsg);
            }
            // Handle other messages (only if authenticated)
            else if (isSessionAuthenticated(session)) {
                handleAuthenticatedMessage(session, wsMsg);
            } else {
                if (attemptLazyAuthentication(session, wsMsg)) {
                    handleAuthenticatedMessage(session, wsMsg);
                } else {
                    log.warn("Received non-AUTH message on unauthenticated session: {}", sessionId);
                    sendErrorMessage(session, "Not authenticated. Send AUTH message first.");
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing WebSocket message on session {}: {}", sessionId, e.getMessage());
            sendErrorMessage(session, "Invalid message format: " + e.getMessage());
        }
    }
    
    /**
     * Handle AUTH handshake message.
     * Client sends: {"type":"AUTH", "userId":123}
     * Server responds: {"type":"CONFIRMED", "userId":123, "payload":{...}}
     */
    private void handleAuthHandshake(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        String sessionId = session.getId();
        
        try {
            // Extract userId from message
            if (message.getUserId() == null) {
                log.warn("AUTH message missing userId on session {}", sessionId);
                sendErrorMessage(session, "AUTH message must include userId");
                session.close(CloseStatus.PROTOCOL_ERROR);
                return;
            }
            
            Long userId = message.getUserId();
            
            if (userId <= 0) {
                log.warn("Invalid userId in AUTH: {}", userId);
                sendErrorMessage(session, "Invalid userId");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            
            // Register the authenticated session
            sessionManager.registerSession(userId, session);
            pendingAuthSessions.remove(sessionId);
            
            // Send confirmation message
            ConfirmedPayload confirmedPayload = new ConfirmedPayload(
                    "WebSocket connection authenticated and ready for real-time updates",
                    sessionId
            );
            WebSocketMessage<ConfirmedPayload> confirmMsg = new WebSocketMessage<>(
                    "CONFIRMED",
                    userId,
                    confirmedPayload
            );
            
            String jsonResponse = objectMapper.writeValueAsString(confirmMsg);
            session.sendMessage(new TextMessage(jsonResponse));
            
            log.info("User {} authenticated on WebSocket session {}", userId, sessionId);
            
        } catch (Exception e) {
            log.error("Error during AUTH handshake on session {}: {}", sessionId, e.getMessage());
            sendErrorMessage(session, "Authentication failed: " + e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                log.error("Error closing session after auth failure: {}", ex.getMessage());
            }
        }
    }

    private boolean attemptLazyAuthentication(WebSocketSession session, WebSocketMessage<?> message) {
        if (message == null || message.getUserId() == null || message.getUserId() <= 0) {
            return false;
        }

        String type = message.getType() != null ? message.getType().toUpperCase() : "";
        if (!"BIN_COLLECTED".equals(type) && !"ROUTE_OPTIMIZE".equals(type)) {
            return false;
        }

        String sessionId = session.getId();
        Long userId = message.getUserId();

        sessionManager.registerSession(userId, session);
        pendingAuthSessions.remove(sessionId);

        log.info("Lazy-authenticated websocket session {} for user {} via message type {}", sessionId, userId, type);

        try {
            ConfirmedPayload confirmedPayload = new ConfirmedPayload(
                    "WebSocket connection authenticated and ready for real-time updates",
                    sessionId
            );
            WebSocketMessage<ConfirmedPayload> confirmMsg = new WebSocketMessage<>(
                    "CONFIRMED",
                    userId,
                    confirmedPayload
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMsg)));
        } catch (Exception ex) {
            log.warn("Failed to send CONFIRMED after lazy-auth: {}", ex.getMessage());
        }

        return true;
    }
    
    /**
     * Handle messages from authenticated clients.
     * (Currently messages come from server broadcasts, not client-initiated)
     */
    private void handleAuthenticatedMessage(WebSocketSession session, WebSocketMessage<?> message) {
        log.debug("Authenticated message received on session {}: type={}", session.getId(), message.getType());

        if ("ROUTE_OPTIMIZE".equalsIgnoreCase(message.getType())) {
            handleRouteOptimizeMessage(session, message);
            return;
        }

        if ("BIN_COLLECTED".equalsIgnoreCase(message.getType())) {
            handleBinCollectedMessage(session, message);
            return;
        }

        sendErrorMessage(session, "Unsupported message type: " + message.getType());
    }

    private void handleBinCollectedMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            Long authenticatedUserId = sessionManager.getUserIdBySessionId(session.getId());
            if (authenticatedUserId == null) {
                sendErrorMessage(session, "Unable to resolve authenticated user for this session.");
                return;
            }

            if (!(message.getPayload() instanceof java.util.Map<?, ?> payloadMap)) {
                sendErrorMessage(session, "BIN_COLLECTED payload is required.");
                return;
            }

            BinCollectedPayload payload = objectMapper.convertValue(payloadMap, BinCollectedPayload.class);
            Long requestUserId = payload.getUserId() != null ? payload.getUserId() : authenticatedUserId;
            if (!authenticatedUserId.equals(requestUserId)) {
                sendErrorMessage(session, "userId does not match authenticated websocket user.");
                return;
            }
            if (payload.getBinId() == null) {
                sendErrorMessage(session, "binId is required for BIN_COLLECTED.");
                return;
            }

            var result = binCollectionRealtimeService.processBinCollected(
                    requestUserId,
                    payload.getBinId(),
                    payload.getPriority(),
                    payload.getBasePoints(),
                    payload.getSessionId()
            );

            WebSocketMessage<java.util.Map<String, Object>> ack = new WebSocketMessage<>(
                    "BIN_COLLECTION_ACK",
                    requestUserId,
                    java.util.Map.of(
                            "userId", result.userId(),
                            "binId", result.binId(),
                            "totalBinsCollected", result.totalBinsCollected(),
                            "affectedTasks", result.affectedTasks(),
                            "updatedAt", System.currentTimeMillis()
                    )
            );

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
            }
        } catch (IllegalArgumentException ex) {
            sendErrorMessage(session, ex.getMessage());
        } catch (Exception ex) {
            log.error("Error handling BIN_COLLECTED: {}", ex.getMessage());
            sendErrorMessage(session, "Failed to process bin collection: " + ex.getMessage());
        }
    }

    private void handleRouteOptimizeMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            Long authenticatedUserId = sessionManager.getUserIdBySessionId(session.getId());
            if (authenticatedUserId == null) {
                sendErrorMessage(session, "Unable to resolve authenticated user for this session.");
                return;
            }

            if (!(message.getPayload() instanceof java.util.Map<?, ?> payloadMap)) {
                sendErrorMessage(session, "ROUTE_OPTIMIZE payload is required.");
                return;
            }

            RouteOptimizeRequestPayload payload = objectMapper.convertValue(
                    payloadMap,
                    RouteOptimizeRequestPayload.class
            );

            Long requestUserId = payload.getUserId() != null ? payload.getUserId() : authenticatedUserId;
            if (!authenticatedUserId.equals(requestUserId)) {
                sendErrorMessage(session, "userId does not match authenticated websocket user.");
                return;
            }

            RouteSessionCreateRequestDTO request = new RouteSessionCreateRequestDTO();
            request.setSessionId(payload.getSessionId());
            request.setUserId(requestUserId);
            request.setVehicleCount(payload.getVehicleCount());
            request.setVehicleCapacities(payload.getVehicleCapacities());
            request.setDepotLat(payload.getDepotLat());
            request.setDepotLng(payload.getDepotLng());
            request.setSelectedBinIds(payload.getSelectedBinIds());

            RouteSessionSnapshotDTO snapshot = routeSessionService.optimizeAndBroadcast(request);
            sendRouteOptimizeAck(session, requestUserId, payload.getSessionId(), snapshot);
            log.info(
                    "Route optimize processed: requestedSessionId={}, sessionId={}, created={}, userId={}, status={}, selectedBins={}",
                    payload.getSessionId(),
                    snapshot.sessionId,
                    payload.getSessionId() == null || payload.getSessionId().isBlank() || !payload.getSessionId().equals(snapshot.sessionId),
                    requestUserId,
                    snapshot.status,
                    snapshot.selectedBinIds
            );
            if ("ERROR".equalsIgnoreCase(snapshot.status)) {
                sendErrorMessage(session, snapshot.message != null ? snapshot.message : "Route optimization failed.");
            } else if ("WARNING".equalsIgnoreCase(snapshot.status)) {
                log.warn("Route optimize warning: {}", snapshot.message);
            }
        } catch (IllegalArgumentException ex) {
            sendErrorMessage(session, ex.getMessage());
        } catch (Exception ex) {
            log.error("Error handling ROUTE_OPTIMIZE: {}", ex.getMessage());
            sendErrorMessage(session, "Failed to optimize route: " + ex.getMessage());
        }
    }

    private void sendRouteOptimizeAck(
            WebSocketSession session,
            Long userId,
            String requestedSessionId,
            RouteSessionSnapshotDTO snapshot
    ) {
        try {
            boolean created = requestedSessionId == null || requestedSessionId.isBlank() || !requestedSessionId.equals(snapshot.sessionId);
            RouteOptimizeAckPayload payload = new RouteOptimizeAckPayload(
                    snapshot.sessionId,
                    snapshot.status,
                    snapshot.message,
                    snapshot.selectedBinIds != null ? snapshot.selectedBinIds : Collections.emptyList(),
                    created
            );

            WebSocketMessage<RouteOptimizeAckPayload> ack = new WebSocketMessage<>(
                    "ROUTE_OPTIMIZE_ACK",
                    userId,
                    payload
            );

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
                log.info(
                        "Route optimize ack sent: requestedSessionId={}, sessionId={}, created={}, userId={}, status={}",
                        requestedSessionId,
                        snapshot.sessionId,
                        created,
                        userId,
                        snapshot.status
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to send ROUTE_OPTIMIZE_ACK: {}", ex.getMessage());
        }
    }
    
    /**
     * Called when a WebSocket connection is closed.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        pendingAuthSessions.remove(sessionId);
        sessionManager.unregisterSessionBySessionId(sessionId);
        
        log.info("WebSocket connection closed: {} - status: {}", sessionId, closeStatus);
    }
    
    /**
     * Called when an error occurs on the WebSocket connection.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket transport error on session {}: {}", sessionId, exception.getMessage());
        pendingAuthSessions.remove(sessionId);
        sessionManager.unregisterSessionBySessionId(sessionId);
    }
    
    /**
     * Check if a session is authenticated (has been registered in sessionManager).
     */
    private boolean isSessionAuthenticated(WebSocketSession session) {
        return !pendingAuthSessions.containsKey(session.getId());
    }
    
    /**
     * Send an error message to the client.
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            WebSocketMessage<Void> errorMsg = new WebSocketMessage<>("ERROR", errorMessage);
            String jsonError = objectMapper.writeValueAsString(errorMsg);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonError));
            }
        } catch (IOException e) {
            log.error("Failed to send error message: {}", e.getMessage());
        }
    }
}
