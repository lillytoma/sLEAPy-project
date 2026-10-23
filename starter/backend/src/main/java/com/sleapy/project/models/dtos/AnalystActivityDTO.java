package com.sleapy.project.models.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnalystActivityDTO {
    private LocalDateTime timestamp;
    private String instrumentType;
    private BigDecimal tradeVolume;
}
