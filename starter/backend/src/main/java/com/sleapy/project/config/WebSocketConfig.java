package com.sleapy.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time price updates.
 * Enables STOMP messaging over WebSocket for broadcasting market prices to connected clients.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for STOMP messaging.
     * - /topic/* : clients subscribe to receive messages
     * - /queue/* : point-to-point messaging (if needed)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints.
     * Clients connect via: ws://localhost:8080/ws/prices
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/prices")
                .setAllowedOrigins("*")  // Allow all origins for development; restrict in production
                .withSockJS();            // Fallback for browsers without native WebSocket support
    }
}
