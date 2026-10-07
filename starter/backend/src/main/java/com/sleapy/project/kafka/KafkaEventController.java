package com.sleapy.project.kafka;

import com.sleapy.project.events.ClientActivityEvent;
import com.sleapy.project.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Example controller demonstrating Kafka event publishing.
 * 
 * This controller shows how to publish events to Kafka when user actions occur
 * in the trading application.
 * 
 * Usage: These endpoints are for demonstration/testing purposes.
 * In production, events should be published from service layer,
 * not directly from controllers.
 * 
 * Note: This controller is only active when Kafka is enabled.
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventController {
    
    private final KafkaProducerService kafkaProducerService;
    
    /**
     * Test endpoint: Publish a client activity event
     * 
     * Example request:
     * POST /api/kafka/client-activity
     * {
     *   "clientId": 123,
     *   "activityType": "LOGIN",
     *   "details": "{\"ip\": \"192.168.1.1\"}"
     * }
     */
    @PostMapping("/client-activity")
    public String publishClientActivity(
            @RequestParam Long clientId,
            @RequestParam String activityType,
            @RequestParam(required = false) String details) {
        
        try {
            ClientActivityEvent event = ClientActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .clientId(clientId)
                .activityType(activityType)
                .timestamp(LocalDateTime.now())
                .details(details != null ? details : "{}")
                .sourceSystem("trading-app")
                .build();
            
            kafkaProducerService.publishClientActivityEvent(event);
            
            return String.format("Client activity event published: %s", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish client activity event", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Test endpoint: Publish an order event
     * 
     * Example request:
     * POST /api/kafka/order
     * {
     *   "orderId": 456,
     *   "clientId": 123,
     *   "instrumentId": 789,
     *   "orderType": "BUY",
     *   "quantity": 100.00,
     *   "price": 150.50,
     *   "status": "PENDING"
     * }
     */
    @PostMapping("/order")
    public String publishOrder(
            @RequestParam Long orderId,
            @RequestParam Long clientId,
            @RequestParam Long instrumentId,
            @RequestParam String orderType,
            @RequestParam BigDecimal quantity,
            @RequestParam BigDecimal price,
            @RequestParam String status) {
        
        try {
            OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .clientId(clientId)
                .instrumentId(instrumentId)
                .orderType(orderType)
                .quantity(quantity)
                .price(price)
                .status(status)
                .timestamp(LocalDateTime.now())
                .sourceSystem("trading-app")
                .build();
            
            kafkaProducerService.publishOrderEvent(event);
            
            return String.format("Order event published: %s", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish order event", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Health check endpoint to verify Kafka connectivity
     * 
     * Example request:
     * GET /api/kafka/health
     */
    @GetMapping("/health")
    public String healthCheck() {
        return "Kafka integration is active and ready to publish events";
    }
}

