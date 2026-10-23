package com.sleapy.project.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AnalystPortfolioDTO(
        LocalDateTime snapshotAt, int portfolios, BigDecimal cashBalance,
        BigDecimal holdingsValue, BigDecimal netWorth, long missingPrices,
        Map<String, BigDecimal> instrumentValues) {
}
