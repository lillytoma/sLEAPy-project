package com.sleapy.project.models.dtos;

import java.math.BigDecimal;

public record AnalystInstrumentActivityDTO(
        String symbol,
        String symbolName,
        String instrumentType,
        long totalTrades,
        BigDecimal tradeVolume) {
}
