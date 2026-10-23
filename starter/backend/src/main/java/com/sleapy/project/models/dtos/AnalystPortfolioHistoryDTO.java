package com.sleapy.project.models.dtos;

import java.time.LocalDateTime;

public record AnalystPortfolioHistoryDTO(
        LocalDateTime historySince, AnalystPortfolioDTO first, AnalystPortfolioDTO latest,
        boolean comparable) {
}
