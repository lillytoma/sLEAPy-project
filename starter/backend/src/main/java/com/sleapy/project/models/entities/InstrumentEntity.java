package com.sleapy.project.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Table(name = "instruments")
@Entity
@Data 
@NoArgsConstructor 
public class InstrumentEntity {
    public InstrumentEntity(Long id, String symbol, String symbolName, InstrumentType instrumentType) {
        this.id = id;
        this.symbol = symbol;
        this.symbolName = symbolName;
        this.instrumentType = instrumentType;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instrument_id")
    private Long id;

    //AAPL
    @Column(name = "symbol")
    private String symbol;

    //Apple Inc.
    @Column(name = "symbol_name")
    private String symbolName;

 

   @ManyToOne 
   @JoinColumn(name = "instrumenttype_id")
    private InstrumentType instrumentType;


}
