package com.sleapy.project.services;

import com.sleapy.project.mappers.AnalystPortfolioMapper;
import com.sleapy.project.mappers.AnalystMapper;
import com.sleapy.project.models.dtos.AnalystPortfolioHistoryDTO;
import com.sleapy.project.models.dtos.AnalystPortfolioRowDTO;
import com.sleapy.project.models.enums.AnalystPeriod;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalystPortfolioServiceTest {
    private final LocalDateTime time = LocalDateTime.parse("2026-10-23T12:00:00");

    private AnalystPortfolioRowDTO row(long client, String type, int cash, int holdings, long missing) {
        var row = new AnalystPortfolioRowDTO();
        row.setSnapshotAt(time);
        row.setClientId(client);
        row.setInstrumentType(type);
        row.setCashBalance(BigDecimal.valueOf(cash));
        row.setHoldingsValue(BigDecimal.valueOf(holdings));
        row.setMissingPrices(missing);
        return row;
    }

    @Test
    void sumsHoldingsWithoutDoubleCountingCashAcrossInstrumentTypes() {
        var summary = AnalystPortfolioService.summarize(List.of(
                row(1, "Stock", 100, 20, 0), row(1, "ETF", 100, 60, 0),
                row(2, "Other", 200, 0, 0)));
        assertEquals(2, summary.portfolios());
        assertEquals(new BigDecimal("300"), summary.cashBalance());
        assertEquals(new BigDecimal("80"), summary.holdingsValue());
        assertEquals(new BigDecimal("380"), summary.netWorth());
        assertEquals(new BigDecimal("60"), summary.instrumentValues().get("ETF"));
    }

    @Test
    void incompleteValuationsAreUnavailableRatherThanZeroAndNoHistoryIsNull() {
        var summary = AnalystPortfolioService.summarize(List.of(
                row(1, "Stock", 100, 20, 1), row(1, "ETF", 100, 60, 0)));
        assertNull(summary.netWorth());
        assertNull(summary.holdingsValue());
        assertNull(summary.instrumentValues().get("Stock"));
        assertEquals(new BigDecimal("60"), summary.instrumentValues().get("ETF"));
        assertEquals(new BigDecimal("100"), summary.cashBalance());
        assertEquals(1, summary.missingPrices());
        assertNull(AnalystPortfolioService.summarize(List.of()));
    }

    @Test
    void historyForwardsRangeAndClientAndDetectsChangedCustomerCohorts() {
        var mapper = mock(AnalystPortfolioMapper.class);
        var service = new AnalystPortfolioService(mapper);
        when(mapper.findSnapshotForClients(List.of(1L), time, time.plusDays(1), false))
                .thenReturn(List.of(row(1, "Stock", 100, 20, 0)));
        when(mapper.findSnapshotForClients(List.of(1L), time, time.plusDays(1), true))
                .thenReturn(List.of(row(2, "Stock", 100, 20, 0)));
        when(mapper.findHistorySinceForClients(List.of(1L))).thenReturn(time.minusDays(1));
        var history = service.getHistory(1L, time, time.plusDays(1));
        assertFalse(history.comparable());
        assertEquals(time.minusDays(1), history.historySince());
        when(mapper.findSnapshotForClients(List.of(1L), time, time.plusDays(1), true))
                .thenReturn(List.of(row(1, "Stock", 100, 30, 0)));
        assertTrue(service.getHistory(1L, time, time.plusDays(1)).comparable());
    }

    @Test
    void captureUsesTheCurrentTimeAndSummaryUsesTheSameSelectedRangeAsTradingQueries() {
        var portfolioMapper = mock(AnalystPortfolioMapper.class);
        var portfolioService = new AnalystPortfolioService(portfolioMapper);
        var before = LocalDateTime.now();
        portfolioService.capture();
        var captured = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(portfolioMapper).capture(captured.capture());
        assertFalse(captured.getValue().isBefore(before));
        assertFalse(captured.getValue().isAfter(LocalDateTime.now()));

        var analystMapper = mock(AnalystMapper.class);
        var historyService = mock(AnalystPortfolioService.class);
        var analystService = new AnalystService(analystMapper, historyService);
        var expected = new AnalystPortfolioHistoryDTO(null, null, null, false);
        when(historyService.getHistoryForClients(eq(List.of(42L)), any(), any())).thenReturn(expected);
        var summary = analystService.getSummary(42L, AnalystPeriod.YEAR);
        assertSame(expected, summary.getPortfolioHistory());
        var start = ArgumentCaptor.forClass(LocalDateTime.class);
        var end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyService).getHistoryForClients(eq(List.of(42L)), start.capture(), end.capture());
        assertEquals(AnalystPeriod.YEAR.startAt(end.getValue()), start.getValue());
        verify(analystMapper).findTradeVolumeForClients(List.of(42L), start.getValue(), end.getValue());
    }
}
