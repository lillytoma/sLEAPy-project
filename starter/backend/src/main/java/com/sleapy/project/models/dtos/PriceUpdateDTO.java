package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for real-time price updates sent via WebSocket.
 * Contains minimal market data for bandwidth efficiency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateDTO {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private LocalDateTime lastUpdated;
}
