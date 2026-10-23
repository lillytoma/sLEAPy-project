package com.sleapy.project.models.dtos;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AnalystInstrumentOwnerDTO {
    private Long clientId;
    private String username;
    private BigDecimal shares;
}
