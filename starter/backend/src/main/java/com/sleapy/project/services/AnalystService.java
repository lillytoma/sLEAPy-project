package com.sleapy.project.services;

import com.sleapy.project.mappers.AnalystMapper;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystInstrumentActivityDTO;
import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalystService {
    private final AnalystMapper analystMapper;

    public AnalystSummaryDTO getSummary(Long clientId) {
        BigDecimal tradeVolume = analystMapper.findTradeVolume(clientId);
        long totalTrades = analystMapper.countTrades(clientId);
        String latestTradeTime = analystMapper.findLatestTradeTime(clientId);
        List<AnalystInstrumentActivityDTO> instruments =
                analystMapper.findMostActiveInstruments(clientId);

        return new AnalystSummaryDTO(
                tradeVolume,
                totalTrades,
                instruments,
                latestTradeTime);
    }

    public List<AnalystClientDTO> searchClients(String search) {
        return analystMapper.searchClients(search.trim());
    }
}
