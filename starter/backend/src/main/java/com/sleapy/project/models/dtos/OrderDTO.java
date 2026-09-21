package com.sleapy.project.models.dtos;



import java.time.LocalDate;


public class OrderDTO{
    public OrderDTO(OrderEntity entity){
        this.instrument = new InstrumentDTO(entity.getInstrument());
        this.timeOfPurchase = entity.getTimeOfPurchase();
        this.quantity = entity.getQuantity();
    }

    public OrderDTO(LocalDate timeOfPurchase, Integer quantity, InstrumentDTO instrument) {
        this.timeOfPurchase = timeOfPurchase;
        this.quantity = quantity;
        this.instrument = instrument;
    }

    private LocalDate timeOfPurchase;
    private Integer quantity;

    public LocalDate getTimeOfPurchase() {
        return timeOfPurchase;
    }

    public void setTimeOfPurchase(LocalDate timeOfPurchase) {
        this.timeOfPurchase = timeOfPurchase;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public InstrumentDTO getInstrument() {
        return instrument;
    }

    public void setInstrument(InstrumentDTO instrument) {
        this.instrument = instrument;
    }

    private InstrumentDTO instrument;
}
