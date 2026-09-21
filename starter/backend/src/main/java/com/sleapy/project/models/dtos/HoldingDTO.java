package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for holding information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class HoldingDTO {
    
    private Long holdingId; // Unique holding identifier
    private InstrumentDTO instrument; // The financial instrument owned
    private ClientDTO client; // The owner of this holding
    private double totalShares; // Total number of shares owned
    private double totalPrice; // Total cost basis (shares × purchase price)
    
}
