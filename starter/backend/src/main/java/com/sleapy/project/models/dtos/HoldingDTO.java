package com.sleapy.project.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Data transfer object for holding information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class HoldingDTO {
    
    private Long holdingId;                   // Unique holding identifier
    private InstrumentDTO instrument;         // The financial instrument owned
    private ClientDTO client;                 // The owner of this holding
    private BigDecimal quantity;              // Number of shares owned
    private BigDecimal costBasis;             // Total cost basis (quantity × purchase_price)
    private BigDecimal currentMarketValue;    // Current market value (quantity × current_price)
    private BigDecimal unrealizedGainLoss;    // Unrealized gain/loss (currentMarketValue - costBasis)
    
    @NotNull(message = "holding id is required")
    @Positive(message = "holding id must be positive")
    private Long id; // Unique holding identifier

    @NotNull(message = "instrument is required")
    @Valid
    private InstrumentDTO instrument; // The financial instrument owned

    @NotNull(message = "client is required")
    @Valid
    private ClientDTO client; // The owner of this holding

    @PositiveOrZero(message = "total shares must be zero or positive")
    private double totalShares; // Total number of shares owned

    @PositiveOrZero(message = "total price must be zero or positive")
    private double totalPrice; // Total cost basis (shares × purchase price)
    
}
