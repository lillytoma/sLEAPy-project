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
    private Long clientId;              // Client making the sale
    private Long instrumentId;          // Stock/instrument to sell
    private BigDecimal quantity;        // Number of shares to sell
    // Note: Price will be fetched from y-finance or current market price
}
