package com.sleapy.project.models.domain;

import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.models.enums.OrderStatus;

/**
 * Domain model representing a buy/sell order for an instrument.
 * Tracks order lifecycle from creation to fulfillment.
 */
public class Order {

    private final Long id; // Unique order identifier
    private final InstrumentDTO instrument; // The instrument being ordered
    private final int quantity; // Number of shares/units
    private final String timePurchased; // Order creation timestamp
    private final double purchasePrice; // Price per unit at time of order

    private String timeFilled; // Timestamp when order was executed
    private OrderStatus status; // Current order state (PENDING, ACCEPTED, FILLED, REJECTED) 

    /**
     * Constructs an Order from a DTO.
     */
    public Order(OrderDTO dto) {
        this.id = dto.getOrderId();
        this.instrument = dto.getInstrument();
        this.quantity = dto.getQuantity();
        this.timePurchased = dto.getTimePurchased();
        this.purchasePrice = dto.getPurchasePrice();
        this.timeFilled = dto.getTimeFilled();
        this.status = dto.getStatus();
    }

    /**
     * Sets the timestamp when the order was filled.
     */
    public void setTimeFilled(String timeFilled) {
        this.timeFilled = timeFilled;
    }

    /**
     * Updates the order status (called during order processing).
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    // Getters
    public Long getId() {
        return this.id;
    }

    public InstrumentDTO getInstrument() {
        return this.instrument;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public String getTimePurchased() {
        return this.timePurchased;
    }

    public double getPurchasePrice() {
        return this.purchasePrice;
    }

    public String getTimeFilled() {
        return this.timeFilled;
    }

    public OrderStatus getStatus() {
        return this.status;
    }
}
