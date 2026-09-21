package com.sleapy.project.models.domain;

import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.enums.InstrumentType;

/**
 * Domain model representing a tradeable financial instrument (e.g., stock, bond).
 * Contains immutable instrument details and mutable market price updated from external APIs.
 */
public class Instrument {
    
    private final String id; // Unique symbol identifier (e.g., AAPL)
    private final String name; // Display name of the instrument (e.g., Apple)
    private final InstrumentType instrumentType; // Classification (BOND, EQUITY, CRYPTO, FUND)
    private double currentMarketPrice; // Current market price (updated from external APIs)

    /**
     * Constructs an Instrument from a DTO (typically from API response).
     */
    public Instrument(InstrumentDTO dto) {
        this.id = dto.getSymbolId();
        this.name = dto.getSymbolName();
        this.instrumentType = dto.getInstrumentType();
        this.currentMarketPrice = dto.getCurrentMarketPrice();
    }

    /**
     * Updates the current market price (called when external API provides new data).
     */
    public void setCurrentMarketPrice(double currentMarketPrice) {
        this.currentMarketPrice = currentMarketPrice;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public InstrumentType getInstrumentType() {
        return this.instrumentType;
    }

    public double getCurrentMarketPrice() {
        return this.currentMarketPrice;
    }

}
