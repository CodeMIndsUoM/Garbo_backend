package com.garbo.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages authenticated WebSocket sessions and provides broadcasting methods.
 * Tracks users by ID and their corresponding WebSocket sessions.
 */
@Slf4j
@Component
public class WebSocketSessionManager {
    
    // userId -> WebSocketSession mapping
    private final ConcurrentHashMap<Long, WebSocketSession> authenticatedSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> sessionIdByUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userIdBySessionId = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Register an authenticated session for a user.
     */
    public void registerSession(Long userId, WebSocketSession session) {
        String sessionId = session.getId();

        // If user already has a session, close the old one
        WebSocketSession oldSession = authenticatedSessions.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                userIdBySessionId.remove(oldSession.getId());
                oldSession.close();
            } catch (IOException e) {
                log.warn("Failed to close old session for user {}: {}", userId, e.getMessage());
            }
        }
        String previousSessionId = sessionIdByUserId.put(userId, sessionId);
        if (previousSessionId != null && !previousSessionId.equals(sessionId)) {
            userIdBySessionId.remove(previousSessionId);
        }
        userIdBySessionId.put(sessionId, userId);
        log.info("WebSocket session registered for user: {}", userId);
    }
    
    /**
     * Unregister a session when user disconnects.
     */
    public void unregisterSession(Long userId) {
        WebSocketSession session = authenticatedSessions.remove(userId);
        String sessionId = sessionIdByUserId.remove(userId);
        if (sessionId != null) {
            userIdBySessionId.remove(sessionId);
        }
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("Error closing session for user {}: {}", userId, e.getMessage());
            }
        }
        log.info("WebSocket session unregistered for user: {}", userId);
    }
    
    /**
     * Get a user's WebSocket session.
     */
    public WebSocketSession getSession(Long userId) {
        return authenticatedSessions.get(userId);
    }

    public Long getUserIdBySessionId(String sessionId) {
        return userIdBySessionId.get(sessionId);
    }

    public void unregisterSessionBySessionId(String sessionId) {
        Long userId = userIdBySessionId.remove(sessionId);
        if (userId == null) {
            return;
        }

        sessionIdByUserId.remove(userId, sessionId);
        WebSocketSession removed = authenticatedSessions.remove(userId);
        if (removed != null && removed.isOpen()) {
            try {
                removed.close();
            } catch (IOException e) {
                log.warn("Error closing session for user {}: {}", userId, e.getMessage());
            }
        }
        log.info("WebSocket session unregistered by sessionId: {} for user {}", sessionId, userId);
    }
    
    /**
     * Check if a user is connected.
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = authenticatedSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * Send a message to a specific user.
     */
    public void sendToUser(Long userId, WebSocketMessage<?> message) {
        WebSocketSession session = authenticatedSessions.get(userId);
        if (session == null) {
            return;
        }

        if (!session.isOpen()) {
            unregisterSession(userId);
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonPayload));
            log.debug("Sent message type {} to user {}", message.getType(), userId);
        } catch (IOException e) {
            log.error("Failed to send message to user {}: {}", userId, e.getMessage());
            unregisterSession(userId);
        }
    }
    
    /**
     * Broadcast a message to all connected users (with filter).
     * Useful for leaderboard updates that should go to everyone.
     */
    public void broadcastToAll(WebSocketMessage<?> message) {
        int successCount = 0;
        int failureCount = 0;
        
        for (Long userId : authenticatedSessions.keySet()) {
            try {
                sendToUser(userId, message);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to broadcast to user {}: {}", userId, e.getMessage());
                failureCount++;
            }
        }
        
        log.info("Broadcast message type {} sent to {}/{} users (failures: {})", 
                message.getType(), successCount, authenticatedSessions.size(), failureCount);
    }
    
    /**
     * Broadcast a message to all connected users except one.
     */
    public void broadcastToAllExcept(Long excludeUserId, WebSocketMessage<?> message) {
        for (Long userId : authenticatedSessions.keySet()) {
            if (!userId.equals(excludeUserId)) {
                try {
                    sendToUser(userId, message);
                } catch (Exception e) {
                    log.warn("Failed to broadcast to user {}: {}", userId, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get count of connected users.
     */
    public int getConnectedUserCount() {
        return authenticatedSessions.size();
    }
    
    /**
     * Clean up all sessions (on application shutdown).
     */
    public void closeAllSessions() {
        for (WebSocketSession session : authenticatedSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.close();
                } catch (IOException e) {
                    log.warn("Error closing session: {}", e.getMessage());
                }
            }
        }
        authenticatedSessions.clear();
        sessionIdByUserId.clear();
        userIdBySessionId.clear();
        log.info("All WebSocket sessions closed");
    }
}
