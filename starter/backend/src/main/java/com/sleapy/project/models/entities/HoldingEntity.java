package com.sleapy.project.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * POJO representing a holding record in the database.
 * Represents client ownership of financial instruments with share quantities and cost basis.
 * Persistence handled by MyBatis.
 */
@Data
@NoArgsConstructor
public class HoldingEntity {
    public HoldingEntity(Long id, ClientEntity client, InstrumentEntity instrument, BigDecimal quantityShares, BigDecimal purchasePrice) {
        this.id = id;
        this.client = client;
        this.instrument = instrument;
        this.quantityShares = quantityShares;
        this.purchasePrice = purchasePrice;
    }

    private Long id;
    private ClientEntity client;
    private InstrumentEntity instrument;
    private BigDecimal quantityShares;
    private BigDecimal purchasePrice;
}
