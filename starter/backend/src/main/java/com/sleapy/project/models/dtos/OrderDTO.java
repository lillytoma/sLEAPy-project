package com.sleapy.project.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sleapy.project.models.entities.OrderEntity;

import lombok.Data;

@Data 
public class OrderDTO{
    public OrderDTO(OrderEntity entity){
        this.instrument = new InstrumentDTO(entity.getInstrument());
        this.timeOfPurchase = entity.getTimeOfPurchase();
        this.quantity = entity.getQuantity();
        this.timeFilled = entity.getTimeFilled();
        this.purchasePrice = entity.getPurchasePrice();
        this.status = entity.getStatus() == null ? null : entity.getStatus().getName();
        this.tradeType = entity.getTradeType();
    }

    public OrderDTO(LocalDate timeOfPurchase, BigDecimal quantity, InstrumentDTO instrument) {
        this.timeOfPurchase = timeOfPurchase;
        this.quantity = quantity;
        this.instrument = instrument;
    }

    private LocalDate timeOfPurchase;
    private BigDecimal quantity;
    private LocalDate timeFilled;
    private BigDecimal purchasePrice;
    private String status;
    private String tradeType;

 

    private InstrumentDTO instrument;
}
