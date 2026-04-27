package com.garbo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import com.garbo.api.websocket.GarboWebSocketHandler;

/**
 * WebSocket configuration for persistent real-time connections.
 * Enables WebSocket endpoint at /ws for route updates and leaderboard broadcasts.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(garboWebSocketHandler(), "/ws")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Bean
    public GarboWebSocketHandler garboWebSocketHandler() {
        return new GarboWebSocketHandler();
    }

}
