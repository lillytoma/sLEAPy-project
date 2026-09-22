package com.sleapy.project.models.dtos;

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
    }

    public OrderDTO(LocalDate timeOfPurchase, Integer quantity, InstrumentDTO instrument) {
        this.timeOfPurchase = timeOfPurchase;
        this.quantity = quantity;
        this.instrument = instrument;
    }

    private LocalDate timeOfPurchase;
    private Integer quantity;
    private LocalDate timeFilled;

 

    private InstrumentDTO instrument;
}
