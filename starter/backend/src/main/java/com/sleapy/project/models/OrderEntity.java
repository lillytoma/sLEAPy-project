package com.sleapy.project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sleapy.project.models.InstrumentEntity;

import java.math.BigDecimal;
import java.time.LocalDate;


@Table(name = "orders")
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {
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
