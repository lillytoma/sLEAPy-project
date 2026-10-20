package com.sleapy.project.models.dtos;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceAlertRequestDTO {
    private Long clientId;
    private String symbol;
    private String direction;
    private BigDecimal changePercent;
    private BigDecimal price;
    private boolean emailEnabled;
    private boolean phoneEnabled;
}
