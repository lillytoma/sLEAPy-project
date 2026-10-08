package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Request DTO for creating a new holding.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHoldingRequestDTO {
    private Long clientId;              // Client who owns the holding
    private Long instrumentId;          // Instrument being held
    private BigDecimal quantity;        // Number of shares
    private BigDecimal purchasePrice;   // Price paid per share
}
