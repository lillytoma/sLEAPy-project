package com.sleapy.project.models.dtos;

import com.sleapy.project.models.entities.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for instrument information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class InstrumentDTO {

    @NotNull(message = "symbol id is required")
    private Long id;
 
    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotBlank(message = "symbol name is required")
    @Size(min = 2, max = 5, message = "symbol name must be 2-5 characters")
    @Pattern(regexp = "^[A-Z]{2,5}$", message = "symbol name must contain only uppercase letters")
    private String symbolName;

    @NotNull(message = "instrument type is required")
    private InstrumentType instrumentType;
}
