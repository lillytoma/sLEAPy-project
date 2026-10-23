package com.sleapy.project.models.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnalystPortfolioRowDTO {
    private LocalDateTime snapshotAt;
    private Long clientId;
    private String instrumentType;
    private BigDecimal cashBalance;
    private BigDecimal holdingsValue;
    private long missingPrices;
}
