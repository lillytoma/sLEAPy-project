package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Request DTO for buying stocks.
 * Contains the client, instrument, and quantity to purchase.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyOrderRequestDTO {
    @com.fasterxml.jackson.annotation.JsonSetter("orderType")
    public void setOrderType(String type) {
        if (!"MARKET".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Only MARKET orders are supported");
        }
    }

    @com.fasterxml.jackson.annotation.JsonSetter("session")
    public void setSession(String session) {
        if (!"REGULAR".equalsIgnoreCase(session)) {
            throw new IllegalArgumentException("Only REGULAR session orders are supported; closed-session orders are queued");
        }
    }

    private Long clientId;              // Client making the purchase
    private Long instrumentId;          // Stock/instrument to buy
    private BigDecimal quantity;        // Number of shares to buy
    // Note: Price will be fetched from y-finance or current market price
}
