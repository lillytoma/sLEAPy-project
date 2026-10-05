package com.sleapy.project.models.dtos;

import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.InstrumentType;
import java.math.BigDecimal;

public class InstrumentDTO {

    private String symbol;
    private String symbolName;
    private InstrumentType instrumentType;
    private BigDecimal currentPrice;

    public InstrumentDTO(InstrumentEntity entity){
        this.symbol = entity.getSymbol();
        this.instrumentType = entity.getInstrumentType();
        this.symbolName = entity.getSymbolName();
        this.currentPrice = entity.getCurrentPrice();
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

    public void setCurrentPrice(BigDecimal currentPrice){
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentPrice(){
        return currentPrice;
    }

}