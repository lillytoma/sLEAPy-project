package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO after a successful buy/sell transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponseDTO {
    private Long orderId;               // Order ID created
    private Long clientId;              // Client who made the trade
    private String symbol;              // Stock symbol
    private String tradeType;           // "BUY" or "SELL"
    private BigDecimal quantity;        // Shares traded
    private BigDecimal pricePerShare;   // Price at execution (from y-finance)
    private BigDecimal totalAmount;     // quantity * pricePerShare
    private LocalDate timestamp;        // When trade was executed
    private String message;             // Success message
}
