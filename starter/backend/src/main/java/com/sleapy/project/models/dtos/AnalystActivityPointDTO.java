package com.sleapy.project.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AnalystActivityPointDTO(LocalDateTime timestamp, BigDecimal tradeVolume,
                                      Map<String, BigDecimal> instrumentVolumes) {}
