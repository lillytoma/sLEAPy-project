package com.sleapy.project.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event model for trading orders.
 * Produced whenever an order is created or updated in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("order_id")
    private Long orderId;
    
    @JsonProperty("client_id")
    private Long clientId;
    
    @JsonProperty("instrument_id")
    private Long instrumentId;
    
    @JsonProperty("order_type")
    private String orderType;  // BUY, SELL
    
    @JsonProperty("quantity")
    private BigDecimal quantity;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("status")
    private String status;  // PENDING, FILLED, CANCELLED
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("source_system")
    private String sourceSystem;  // e.g., "trading-app"
}
