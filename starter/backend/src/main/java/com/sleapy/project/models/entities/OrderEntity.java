package com.sleapy.project.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderEntity {
    public OrderEntity(Long id,
                       BigDecimal quantity,
                       LocalDateTime timeOfPurchase,
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
    private BigDecimal quantity;
    private LocalDateTime timeOfPurchase;
    private LocalDateTime timeFilled;
    private BigDecimal purchasePrice;
    private String tradeType;
    private OrderStatus status;
    private ClientEntity client;
    private InstrumentEntity instrument;
}
