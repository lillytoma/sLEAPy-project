package com.sleapy.project.models.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnalystOrderDirectionDTO {
    private String direction;
    private String instrumentType;
    private long totalTrades;
    private BigDecimal tradeVolume;
}
