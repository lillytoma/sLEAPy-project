package com.sleapy.project.kafka;

import com.sleapy.project.events.ClientActivityEvent;
import com.sleapy.project.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaProducerService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${spring.kafka.topics.client-activity:client-activity}")
    private String clientActivityTopic;
    
    @Value("${spring.kafka.topics.orders:orders}")
    private String ordersTopic;
    
    /**
     * Publish a client activity event to Kafka.
     * 
     * @param event The client activity event to publish
     */
    public void publishClientActivityEvent(ClientActivityEvent event) {
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
    public void publishOrderEvent(OrderEvent event) {
        try {
            log.info("Publishing order event: {}", event.getEventId());
            kafkaTemplate.send(ordersTopic, event.getOrderId().toString(), event);
            log.debug("Successfully published order event to topic: {}", ordersTopic);
        } catch (Exception e) {
            log.error("Error publishing order event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
    
    /**
     * Publish a generic message to a specified topic.
     * Useful for custom event types.
     * 
     * @param topic The Kafka topic to publish to
     * @param key The message key
     * @param message The message payload
     */
    public void publishMessage(String topic, String key, Object message) {
        try {
            log.info("Publishing message to topic: {}", topic);
            kafkaTemplate.send(topic, key, message);
            log.debug("Successfully published message to topic: {}", topic);
        } catch (Exception e) {
            log.error("Error publishing message to topic: {}", topic, e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }
}

