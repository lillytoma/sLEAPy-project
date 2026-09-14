package com.sleapy.project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "instruments")
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentEntity {
    @Id
    private Long id;

    //AAPL
    @Column(name = "symbol")
    private String symbol;

    //Apple Inc.
    @Column(name = "symbol_name")
    private String symbolName;


    @Enumerated(EnumType.ORDINAL)
    private InstrumentType instrumentType;


}
