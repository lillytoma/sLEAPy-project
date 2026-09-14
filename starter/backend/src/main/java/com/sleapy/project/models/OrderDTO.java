package com.sleapy.project.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class OrderDTO{
    public OrderDTO(OrderEntity entity){
        this.instrument = new InstrumentDTO(entity.getInstrument());
        this.timeOfPurchase = entity.getTimeOfPurchase();
        this.quantity = entity.getQuantity();
    }
    private LocalDate timeOfPurchase;
    private Integer quantity;
    private InstrumentDTO instrument;
}
