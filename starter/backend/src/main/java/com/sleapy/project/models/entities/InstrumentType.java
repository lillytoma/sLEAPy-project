package com.sleapy.project.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "Instrument_Type")
@Data 
@NoArgsConstructor 
public class InstrumentType {
    // STOCK(1),
    // ETF(2),
    // BOND(3);

        @Id
        @Column(name = "InstrumentType_Id")
    private Long instrumentType_id;

    @Column(name = "InstrumentType")
    private String name;
}
