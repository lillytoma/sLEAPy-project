package com.sleapy.project.models.dtos;

import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.InstrumentType;
import com.sleapy.project.models.entities.MarketPriceEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for instrument with current market price.
 * Combines InstrumentEntity and MarketPriceEntity data for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentWithPriceDTO {
    private String symbol;
    private String symbolName;
    private InstrumentType instrumentType;
    private BigDecimal currentPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private LocalDateTime lastUpdated;

    /**
     * Create DTO from InstrumentEntity and MarketPriceEntity.
     */
    public InstrumentWithPriceDTO(InstrumentEntity instrument, MarketPriceEntity marketPrice) {
        this.symbol = instrument.getSymbol();
        this.symbolName = instrument.getSymbolName();
        this.instrumentType = instrument.getInstrumentType();
        
        if (marketPrice != null) {
            this.currentPrice = marketPrice.getCurrentPrice();
            this.highPrice = marketPrice.getHighPrice();
            this.lowPrice = marketPrice.getLowPrice();
            this.lastUpdated = marketPrice.getLastUpdated();
        }
    }
}
