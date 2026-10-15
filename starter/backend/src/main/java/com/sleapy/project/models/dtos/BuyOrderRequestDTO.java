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
    private Long clientId;              // Client making the purchase
    private Long instrumentId;          // Stock/instrument to buy
    private BigDecimal quantity;        // Number of shares to buy
    // Note: Price will be fetched from y-finance or current market price
}
