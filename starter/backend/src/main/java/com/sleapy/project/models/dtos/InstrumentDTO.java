package com.sleapy.project.models.dtos;

import com.sleapy.project.models.enums.InstrumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Data transfer object for instrument information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class InstrumentDTO {

    private String symbol;
    private String symbolName;
    private InstrumentType instrumentType;
    private BigDecimal currentPrice;

}
