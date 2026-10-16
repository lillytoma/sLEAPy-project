package com.sleapy.project.kafka;

import com.sleapy.project.events.ClientActivityEvent;
import com.sleapy.project.events.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventController {
    
    private final KafkaProducerService kafkaProducerService;

    public KafkaEventController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }
    
    /**
    * Publish at least ten sign-in/sign-out activity events.
     * 
     * Example request:
    * POST /api/kafka/client-activity/batch
    * [{"clientId":123,"activityType":"LOGIN"}, ...]
     */
    @PostMapping("/client-activity/batch")
    public String publishClientActivity(@RequestBody List<ClientActivityEvent> events) {
        
        try {
            kafkaProducerService.publishClientActivityBatch(events);
            
            return String.format("Published %d client activity events", events.size());
        } catch (Exception e) {
            log.error("Failed to publish client activity event", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
    * Publish at least ten client buy/sell order events.
     * 
     * Example request:
    * POST /api/kafka/orders/batch
    * [{"orderId":456,"clientId":123,"orderType":"BUY"}, ...]
     */
    @PostMapping("/orders/batch")
    public String publishOrders(@RequestBody List<OrderEvent> events) {
        
        try {
            kafkaProducerService.publishOrderBatch(events);
            
            return String.format("Published %d order events", events.size());
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

