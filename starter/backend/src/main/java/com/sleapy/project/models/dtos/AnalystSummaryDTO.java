package com.sleapy.project.models.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class AnalystSummaryDTO {
    private BigDecimal tradeVolume;
    private long totalTrades;
    private List<AnalystInstrumentActivityDTO> mostActiveInstruments;
    private String updatedAt;
    private List<AnalystActivityPointDTO> activity = List.of();
    private List<AnalystOrderDirectionDTO> orderDirections = List.of();
    private AnalystPortfolioHistoryDTO portfolioHistory;

    public AnalystSummaryDTO(BigDecimal tradeVolume, long totalTrades,
                             List<AnalystInstrumentActivityDTO> mostActiveInstruments, String updatedAt) {
        this.tradeVolume = tradeVolume;
        this.totalTrades = totalTrades;
        this.mostActiveInstruments = mostActiveInstruments;
        this.updatedAt = updatedAt;
    }
}
