package com.sleapy.project.models.dtos;

import com.sleapy.project.models.enums.InstrumentType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for instrument (stock, bond, etc.) information.
 * Used for API requests/responses and data exchange with external APIs.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class InstrumentDTO {
     
    private String symbolId; // Unique symbol identifier (e.g., AAPL)
    private String symbolName; // Display name (e.g., Apple Inc.)
    private InstrumentType instrumentType; // Classification (BOND, EQUITY, CRYPTO, FUND)
    private double currentMarketPrice; // Current market price (from external APIs)
}
