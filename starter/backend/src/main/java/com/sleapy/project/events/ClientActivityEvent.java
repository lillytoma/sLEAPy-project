package com.sleapy.project.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event model for client activity within the trading application.
 * These events are produced by the application and consumed by the ETL pipeline
 * for data processing and analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientActivityEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("client_id")
    private Long clientId;
    
    @JsonProperty("activity_type")
    private String activityType;  // e.g., LOGIN, LOGOUT, TRADE, ORDER_PLACED
    
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")
    private LocalDateTime timestamp;
    
    @JsonProperty("details")
    private String details;  // JSON string with additional details
    
    @JsonProperty("ip_address")
    private String ipAddress;  // Client IP address
    
    @JsonProperty("source_system")
    private String sourceSystem;  // e.g., "trading-app"
}
