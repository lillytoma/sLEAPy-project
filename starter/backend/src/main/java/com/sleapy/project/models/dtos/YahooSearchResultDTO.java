package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YahooSearchResultDTO {
    private String symbol;
    private String symbolName;
    private String exchange;
    private String quoteType;
    private BigDecimal currentPrice;
}