package com.sleapy.project.models.dtos;

import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.InstrumentType;

public class InstrumentDTO {
    InstrumentDTO(InstrumentEntity entity){
        this.symbol = entity.getSymbol();
        this.instrumentType = entity.getInstrumentType();
        this.symbolName = entity.getSymbolName();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    private String symbol;
    private String symbolName;
    private InstrumentType instrumentType;
}
