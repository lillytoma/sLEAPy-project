package com.sleapy.project.models.dtos;

import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.InstrumentType;
import java.math.BigDecimal;
import com.sleapy.project.models.enums.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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


    @NotNull(message = "symbol id is required")
    @Positive(message = "symbol id must be positive")
    private Long id;
 
    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotBlank(message = "symbol name is required")
    @Size(min = 2, max = 5, message = "symbol name must be 2-5 characters")
    @Pattern(regexp = "^[A-Z]{2,5}$", message = "symbol name must contain only uppercase letters")
    private String symbolName;

    @NotNull(message = "instrument type is required")
    private InstrumentType instrumentType;
}
