package com.sleapy.project.services;

import com.sleapy.project.mappers.AnalystMapper;
import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalystService {
    private final AnalystMapper analystMapper;

    public AnalystSummaryDTO getSummary() {
        return getSummary(null);
    }

    public AnalystSummaryDTO getSummary(Long clientId) {
        BigDecimal tradeVolume = clientId == null
                ? analystMapper.findTradeVolume()
                : analystMapper.findTradeVolumeByClient(clientId);
        long totalTrades = clientId == null
                ? analystMapper.findTotalTrades()
                : analystMapper.findTotalTradesByClient(clientId);
        var instruments = clientId == null
                ? analystMapper.findMostActiveInstruments()
                : analystMapper.findMostActiveInstrumentsByClient(clientId);
        return new AnalystSummaryDTO(
                valueOrZero(tradeVolume),
                totalTrades,
                instruments,
                Instant.now());
    }

    public List<AnalystClientDTO> findClients(String search) {
        return analystMapper.findClients(search == null ? "" : search.trim());
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
