package com.sleapy.project.services;

import com.sleapy.project.mappers.AnalystMapper;
import com.sleapy.project.models.dtos.AnalystActivityDTO;
import com.sleapy.project.models.dtos.AnalystActivityPointDTO;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystInstrumentActivityDTO;
import com.sleapy.project.models.dtos.AnalystInstrumentOwnerDTO;
import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.models.enums.AnalystPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalystService {
    private final AnalystMapper analystMapper;
    private final AnalystPortfolioService portfolioService;

    public List<AnalystInstrumentOwnerDTO> getInstrumentOwners(
            String symbol, List<Long> clientIds) {
        return analystMapper.findInstrumentOwners(symbol, clientIds);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalystSummaryDTO getSummary(Long clientId, AnalystPeriod period) {
        return getSummaryForClients(clientId == null ? List.of() : List.of(clientId), period);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalystSummaryDTO getSummaryForClients(List<Long> clientIds, AnalystPeriod period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = period.startAt(end);
        BigDecimal tradeVolume = analystMapper.findTradeVolumeForClients(clientIds, start, end);
        long totalTrades = analystMapper.countTradesForClients(clientIds, start, end);
        String latestTradeTime = analystMapper.findLatestTradeTimeForClients(clientIds, start, end);
        List<AnalystInstrumentActivityDTO> instruments =
                analystMapper.findMostActiveInstrumentsForClients(clientIds, start, end);

        AnalystSummaryDTO summary = new AnalystSummaryDTO(
                tradeVolume,
                totalTrades,
                instruments,
                latestTradeTime);
        summary.setActivity(buildActivity(analystMapper.findActivityForClients(clientIds, start, end), period, start, end));
        summary.setOrderDirections(analystMapper.findOrderDirectionsForClients(clientIds, start, end));
        summary.setPortfolioHistory(portfolioService.getHistoryForClients(clientIds, start, end));
        return summary;
    }

    static List<AnalystActivityPointDTO> buildActivity(List<AnalystActivityDTO> rows, AnalystPeriod period,
                                                      LocalDateTime start, LocalDateTime end) {
        Map<LocalDateTime, Map<String, BigDecimal>> buckets = new LinkedHashMap<>();
        for (LocalDateTime time = start; !time.isAfter(bucketTime(end, period)); ) {
            buckets.put(time, new LinkedHashMap<>());
            time = switch (period) {
                case DAY -> time.plusHours(1);
                case WEEK, MONTH -> time.plusDays(1);
                case YEAR -> time.plusMonths(1);
            };
        }
        for (AnalystActivityDTO row : rows) {
            var volumes = buckets.get(bucketTime(row.getTimestamp(), period));
            if (volumes != null) volumes.merge(row.getInstrumentType(), row.getTradeVolume(), BigDecimal::add);
        }
        List<AnalystActivityPointDTO> points = new ArrayList<>();
        buckets.forEach((time, volumes) -> points.add(new AnalystActivityPointDTO(time,
                volumes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), volumes)));
        return points;
    }

    private static LocalDateTime bucketTime(LocalDateTime timestamp, AnalystPeriod period) {
        return switch (period) {
            case DAY -> timestamp.truncatedTo(ChronoUnit.HOURS);
            case WEEK, MONTH -> timestamp.toLocalDate().atStartOfDay();
            case YEAR -> timestamp.toLocalDate().withDayOfMonth(1).atStartOfDay();
        };
    }

    public List<AnalystClientDTO> searchClients(String search) {
        return analystMapper.searchClients(search.trim());
    }
}
