package com.sleapy.project.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderEntity {
    public OrderEntity(Long id,
                       Integer quantity,
                       LocalDate timeOfPurchase,
                       BigDecimal purchasePrice,
                       OrderStatus status,
                       ClientEntity client,
                       InstrumentEntity instrument) {
        this.id = id;
        this.quantity = quantity;
        this.timeOfPurchase = timeOfPurchase;
        this.purchasePrice = purchasePrice;
        this.status = status;
        this.client = client;
        this.instrument = instrument;
    }

    public OrderEntity() {
    }

    private Long id;
    private Integer quantity;
    private LocalDate timeOfPurchase;
    private LocalDate timeFilled;
    private BigDecimal purchasePrice;
    private OrderStatus status;
    private ClientEntity client;
    private InstrumentEntity instrument;
}
