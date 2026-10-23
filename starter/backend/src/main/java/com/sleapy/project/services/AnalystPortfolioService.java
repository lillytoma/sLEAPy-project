package com.sleapy.project.services;

import com.sleapy.project.mappers.AnalystPortfolioMapper;
import com.sleapy.project.models.dtos.AnalystPortfolioDTO;
import com.sleapy.project.models.dtos.AnalystPortfolioHistoryDTO;
import com.sleapy.project.models.dtos.AnalystPortfolioRowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalystPortfolioService {
    private final AnalystPortfolioMapper mapper;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void capture() {
        mapper.capture(LocalDateTime.now());
    }

    public AnalystPortfolioHistoryDTO getHistory(Long clientId, LocalDateTime start, LocalDateTime end) {
        return getHistoryForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    public AnalystPortfolioHistoryDTO getHistoryForClients(List<Long> clientIds, LocalDateTime start, LocalDateTime end) {
        var first = mapper.findSnapshotForClients(clientIds, start, end, false);
        var latest = mapper.findSnapshotForClients(clientIds, start, end, true);
        var firstClients = first.stream().map(AnalystPortfolioRowDTO::getClientId).collect(Collectors.toSet());
        var latestClients = latest.stream().map(AnalystPortfolioRowDTO::getClientId).collect(Collectors.toSet());
        return new AnalystPortfolioHistoryDTO(mapper.findHistorySinceForClients(clientIds),
                summarize(first), summarize(latest), !first.isEmpty() && firstClients.equals(latestClients));
    }

    static AnalystPortfolioDTO summarize(List<AnalystPortfolioRowDTO> rows) {
        if (rows.isEmpty()) return null;
        Map<Long, BigDecimal> cashByClient = new LinkedHashMap<>();
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        Map<String, Long> missingByType = new LinkedHashMap<>();
        for (var row : rows) {
            cashByClient.putIfAbsent(row.getClientId(), row.getCashBalance());
            values.merge(row.getInstrumentType(), row.getHoldingsValue(), BigDecimal::add);
            missingByType.merge(row.getInstrumentType(), row.getMissingPrices(), Long::sum);
        }
        var cash = cashByClient.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        var holdings = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long missing = missingByType.values().stream().mapToLong(Long::longValue).sum();
        missingByType.forEach((type, count) -> {
            if (count > 0) values.put(type, null);
        });
        return new AnalystPortfolioDTO(rows.getFirst().getSnapshotAt(), cashByClient.size(), cash,
                missing > 0 ? null : holdings, missing > 0 ? null : holdings.add(cash), missing, values);
    }
}
