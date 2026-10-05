package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Request DTO for updating an existing holding.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHoldingRequestDTO {
    private BigDecimal quantity;        // Updated number of shares
    private BigDecimal purchasePrice;   // Updated price per share
}
