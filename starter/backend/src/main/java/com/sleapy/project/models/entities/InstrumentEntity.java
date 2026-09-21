package com.sleapy.project.models.entities;

import jakarta.persistence.*;


@Table(name = "instruments")
@Entity
public class InstrumentEntity {
    public InstrumentEntity(Long id, String symbol, String symbolName, InstrumentType instrumentType) {
        this.id = id;
        this.symbol = symbol;
        this.symbolName = symbolName;
        this.instrumentType = instrumentType;
    }

    public InstrumentEntity() {
    }

    @Id
    private Long id;

    //AAPL
    @Column(name = "symbol")
    private String symbol;

    //Apple Inc.
    @Column(name = "symbol_name")
    private String symbolName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Enumerated(EnumType.ORDINAL)
    private InstrumentType instrumentType;


}
