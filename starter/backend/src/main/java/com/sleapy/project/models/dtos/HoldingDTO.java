package com.sleapy.project.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;

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
    
    @NotNull(message = "holding id is required")
    private Long id; // Unique holding identifier

    @NotNull(message = "instrument is required")
    @Valid
    private InstrumentDTO instrument; // The financial instrument owned

    @NotNull(message = "client is required")
    @Valid
    private ClientDTO client; // The owner of this holding

    @NotNull(message = "total shares cannot be null")
    @PositiveOrZero(message = "total shares must be zero or positive")
    private double totalShares; // Total number of shares owned

    @NotNull(message = "total price cannot be null")
    @PositiveOrZero(message = "total price must be zero or positive")
    private double totalPrice; // Total cost basis (shares × purchase price)
    
}
