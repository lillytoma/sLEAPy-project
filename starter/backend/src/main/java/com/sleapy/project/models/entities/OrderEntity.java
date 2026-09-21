package com.sleapy.project.models.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Table(name = "orders")
@Entity
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

   

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "time_of_purchase")
    private LocalDate timeOfPurchase;

    @Column(name = "time_filled")
    private LocalDate timeFilled;



    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @ManyToOne
    @JoinColumn(name = "status")
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "instrument_id")
    private InstrumentEntity instrument;

}
