package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.RouteUpdatePayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Broadcasts route optimization updates to relevant collectors.
 * Listens for route changes and pushes real-time bin order updates.
 */
@Slf4j
@Component
public class RouteBroadcaster {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * Broadcast route update to a specific collector.
     */
    public void broadcastRouteUpdateToUser(Long userId, RouteUpdatePayload payload) {
        try {
            WebSocketMessage<RouteUpdatePayload> message = new WebSocketMessage<>(
                    "ROUTE_UPDATE",
                    userId,
                    payload
            );
            
            sessionManager.sendToUser(userId, message);
            log.info("Broadcast route update to user {}", userId);
            
        } catch (Exception e) {
            log.error("Error broadcasting route update to user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Broadcast route update to all collection team members.
     */
    public void broadcastRouteUpdateToAll(RouteUpdatePayload payload) {
        try {
            WebSocketMessage<RouteUpdatePayload> message = new WebSocketMessage<>(
                    "ROUTE_UPDATE",
                    null,
                    payload
            );
            
            sessionManager.broadcastToAll(message);
            log.info("Broadcast route update to all {} connected users",
                    sessionManager.getConnectedUserCount());
            
        } catch (Exception e) {
            log.error("Error broadcasting route update to all users: {}", e.getMessage());
        }
    }
    
    /**
     * Called when a route is optimized/updated.
     * @param userId Collector ID whose route was optimized
     * @param payload Route update payload with bin sequence
     */
    public void onRouteOptimized(Long userId, RouteUpdatePayload payload) {
        try {
            log.info("Route optimized for user {}. Broadcasting update.", userId);
            if (userId != null) {
                if (sessionManager.isUserConnected(userId)) {
                    broadcastRouteUpdateToUser(userId, payload);
                } else {
                    // Fallback for userId mismatch/missing session: still deliver to active clients.
                    log.warn("User {} is not connected. Broadcasting route update to all connected users instead.", userId);
                    broadcastRouteUpdateToAll(payload);
                }
            } else {
                broadcastRouteUpdateToAll(payload);
            }
        } catch (Exception e) {
            log.error("Error handling route optimization for user {}: {}", userId, e.getMessage());
        }
    }
}
