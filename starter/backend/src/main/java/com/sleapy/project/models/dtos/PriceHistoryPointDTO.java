package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for a single historical market-price point.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryPointDTO {
    private String date;
    private BigDecimal close;
}
//comment