package com.sleapy.project.models;

import lombok.Data;

@Data
public class InstrumentDTO {
    InstrumentDTO(InstrumentEntity entity){
        this.symbol = entity.getSymbol();
        this.instrumentType = entity.getInstrumentType();
        this.symbolName = entity.getSymbolName();
    }
    private String symbol;
    private String symbolName;
    private InstrumentType instrumentType;
}
