package com.sleapy.project.models;

import jakarta.persistence.*;

import com.sleapy.project.models.InstrumentEntity;

import java.math.BigDecimal;
import java.time.LocalDate;


@Table(name = "orders")
@Entity

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getTimeOfPurchase() {
        return timeOfPurchase;
    }

    public void setTimeOfPurchase(LocalDate timeOfPurchase) {
        this.timeOfPurchase = timeOfPurchase;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public InstrumentEntity getInstrument() {
        return instrument;
    }

    public void setInstrument(InstrumentEntity instrument) {
        this.instrument = instrument;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "time_of_purchase")
    private LocalDate timeOfPurchase;

    @Column(name = "purchace_price")
    private BigDecimal purchasePrice;

    @Enumerated(EnumType.ORDINAL)
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "instrument_id")
    private InstrumentEntity instrument;

}
