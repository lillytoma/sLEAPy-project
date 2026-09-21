package com.sleapy.project.models.dtos;

import com.sleapy.project.models.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for order information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class OrderDTO {
 
    private Long orderId; // Unique order identifier
    private InstrumentDTO instrument; // The instrument being ordered
    private int quantity; // Number of shares/units
    private String timePurchased; // Order creation timestamp
    private String timeFilled; // Order execution timestamp (null if not filled)
    private double purchasePrice; // Price per unit
    private OrderStatus status; // Current order state 
    
}
