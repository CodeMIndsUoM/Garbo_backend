package com.garbo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import com.garbo.api.websocket.GarboWebSocketHandler;

/**
 * WebSocket configuration for raw real-time connections.
 * Note: STOMP connections are handled in StompWebSocketConfig at /ws.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Raw websocket handler moved to /ws-raw to avoid conflict with STOMP.
        registry.addHandler(garboWebSocketHandler(), "/ws-raw")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Bean
    public GarboWebSocketHandler garboWebSocketHandler() {
        return new GarboWebSocketHandler();
    }

}
