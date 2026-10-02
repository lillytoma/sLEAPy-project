package com.sleapy.project.models.entities;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InstrumentEntity {
    public InstrumentEntity(Long id, String symbol, String symbolName, InstrumentTypeEntity instrumentType) {
        this.id = id;
        this.symbol = symbol;
        this.symbolName = symbolName;
        this.instrumentType = instrumentType;
    }


    private Long id; // AAPL
    private String symbol; // Apple Inc.
    private String symbolName; // instrument type lookup
    private InstrumentTypeEntity instrumentType;
    private BigDecimal currentPrice; //current market price of the instrument


}
