package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalystInstrumentActivityDTO {
    private String symbol;
    private String symbolName;
    private String instrumentType;
    private long totalTrades;
    private BigDecimal tradeVolume;
}
