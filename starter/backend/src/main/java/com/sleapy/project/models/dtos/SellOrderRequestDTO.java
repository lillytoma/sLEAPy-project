package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Request DTO for selling stocks.
 * Contains the client, instrument, and quantity to sell.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellOrderRequestDTO {
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

    private Long clientId;              // Client making the sale
    private Long instrumentId;          // Stock/instrument to sell
    private BigDecimal quantity;        // Number of shares to sell
    // Note: Price will be fetched from y-finance or current market price
}
