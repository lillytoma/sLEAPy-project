package com.sleapy.project.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceEntity {
    private String symbol;                // AAPL, MSFT, etc.
    private BigDecimal currentPrice;      // Latest market price
    private BigDecimal highPrice;         // Daily high
    private BigDecimal lowPrice;          // Daily low
    private LocalDateTime lastUpdated;    // When the price was last refreshed
}
