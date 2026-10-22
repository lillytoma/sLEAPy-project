package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalystSummaryDTO {
    private BigDecimal tradeVolume;
    private long totalTrades;
    private List<AnalystInstrumentActivityDTO> mostActiveInstruments;
    private String updatedAt;
}
