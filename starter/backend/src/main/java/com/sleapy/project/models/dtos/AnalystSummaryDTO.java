package com.sleapy.project.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AnalystSummaryDTO(
        BigDecimal tradeVolume,
        long totalTrades,
        List<AnalystInstrumentActivityDTO> mostActiveInstruments,
        Instant updatedAt) {
}
