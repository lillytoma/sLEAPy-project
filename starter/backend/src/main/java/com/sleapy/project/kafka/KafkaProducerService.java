package com.sleapy.project.kafka;

import com.sleapy.project.events.ClientActivityEvent;
import com.sleapy.project.events.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;

/**
 * Kafka Producer service for publishing events from the trading application.
 * 
 * This service is responsible for sending events to Kafka topics
 * which are then consumed by the ETL pipeline for data processing.
 * 
 * Note: This service is only created when KafkaTemplate bean is available.
 */
@Slf4j
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaProducerService {

    private static final int MIN_BATCH_SIZE = 10;
    
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Value("${spring.kafka.topics.client-activity:client_activity}")
    private String clientActivityTopic;
    
    @Value("${spring.kafka.topics.orders:order_placement}")
    private String orderPlacementTopic;
    
    /**
     * Publish a client activity event to Kafka.
     * 
     * @param event The client activity event to publish
     */
    private void publishClientActivityEvent(ClientActivityEvent event) {
        try {
            log.info("Publishing client activity event: {}", event.getEventId());
            kafkaTemplate.send(clientActivityTopic, event.getClientId().toString(), event);
            log.debug("Successfully published client activity event to topic: {}", clientActivityTopic);
        } catch (Exception e) {
            log.error("Error publishing client activity event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish client activity event", e);
        }
    }
    
    /**
     * Publish an order event to Kafka.
     * 
     * @param event The order event to publish
     */
    private void publishOrderEvent(OrderEvent event) {
        try {
            log.info("Publishing order event: {}", event.getEventId());
            kafkaTemplate.send(orderPlacementTopic, event.getOrderId().toString(), event);
            log.debug("Successfully published order event to topic: {}", orderPlacementTopic);
        } catch (Exception e) {
            log.error("Error publishing order event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }

    public void publishClientActivityBatch(List<ClientActivityEvent> events) {
        requireMinimumBatch(events, "client activity");
        events.forEach(this::validateClientActivity);
        events.forEach(this::publishClientActivityEvent);
    }

    public void publishOrderBatch(List<OrderEvent> events) {
        requireMinimumBatch(events, "orders");
        events.forEach(this::validateOrder);
        events.forEach(this::publishOrderEvent);
    }

    private void validateClientActivity(ClientActivityEvent event) {
        if (event == null || event.getActivityType() == null) {
            throw new IllegalArgumentException("Client activity events must specify LOGIN or LOGOUT");
        }

        String activityType = event.getActivityType().toUpperCase(Locale.ROOT);
        if (!activityType.equals("LOGIN") && !activityType.equals("LOGOUT")) {
            throw new IllegalArgumentException("Only LOGIN and LOGOUT events may be published");
        }
    }

    private void validateOrder(OrderEvent event) {
        if (event == null || event.getOrderType() == null) {
            throw new IllegalArgumentException("Order events must specify BUY or SELL");
        }

        String orderType = event.getOrderType().toUpperCase(Locale.ROOT);
        if (!orderType.equals("BUY") && !orderType.equals("SELL")) {
            throw new IllegalArgumentException("Only BUY and SELL orders may be published");
        }
    }

    private void requireMinimumBatch(List<?> events, String eventType) {
        if (events == null || events.size() < MIN_BATCH_SIZE) {
            throw new IllegalArgumentException(
                String.format("At least %d %s events are required", MIN_BATCH_SIZE, eventType)
            );
        }
    }

}

