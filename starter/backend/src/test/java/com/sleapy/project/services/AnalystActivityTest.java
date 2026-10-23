package com.sleapy.project.services;

import com.sleapy.project.models.dtos.AnalystActivityDTO;
import com.sleapy.project.models.enums.AnalystPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalystActivityTest {
    private final LocalDateTime now = LocalDateTime.parse("2026-10-23T14:07:56");

    private AnalystActivityDTO row(String timestamp, String type, String volume) {
        var row = new AnalystActivityDTO();
        row.setTimestamp(LocalDateTime.parse(timestamp));
        row.setInstrumentType(type);
        row.setTradeVolume(new BigDecimal(volume));
        return row;
    }

    @Test
    void dayBucketsUseHoursAndSumAllTradesWithinTheHour() {
        var points = AnalystService.buildActivity(List.of(
                row("2026-10-23T09:05:00", "Stock", "100"),
                row("2026-10-23T09:45:00", "Stock", "50"),
                row("2026-10-23T09:55:00", "ETF", "25"),
                row("2026-10-23T14:00:00", "Stock", "10")),
                AnalystPeriod.DAY, AnalystPeriod.DAY.startAt(now), now);
        assertEquals(15, points.size());
        assertEquals(now.toLocalDate().atStartOfDay(), points.getFirst().timestamp());
        assertEquals(BigDecimal.ZERO, points.getFirst().tradeVolume());
        assertEquals(new BigDecimal("175"), points.get(9).tradeVolume());
        assertEquals(new BigDecimal("150"), points.get(9).instrumentVolumes().get("Stock"));
        assertEquals(new BigDecimal("25"), points.get(9).instrumentVolumes().get("ETF"));
        assertEquals(new BigDecimal("10"), points.getLast().tradeVolume());
        assertEquals(14, points.getLast().timestamp().getHour());
    }

    @Test
    void weekAndMonthIncludeEveryDateIncludingEmptyDays() {
        for (var period : List.of(AnalystPeriod.WEEK, AnalystPeriod.MONTH)) {
            var points = AnalystService.buildActivity(
                    List.of(row("2026-10-23T09:05:00", "Stock", "100")),
                    period, period.startAt(now), now);
            assertEquals(period == AnalystPeriod.WEEK ? 5 : 23, points.size());
            assertEquals(period.startAt(now), points.getFirst().timestamp());
            assertEquals(new BigDecimal("100"), points.getLast().tradeVolume());
            assertEquals(BigDecimal.ZERO, points.getFirst().tradeVolume());
            for (int i = 1; i < points.size(); i++) {
                assertEquals(points.get(i - 1).timestamp().plusDays(1), points.get(i).timestamp());
            }
        }
    }

    @Test
    void yearUsesMonthsAndDoesNotIncludeFutureMonths() {
        var points = AnalystService.buildActivity(List.of(
                row("2026-01-01T00:00:00", "Stock", "100"),
                row("2026-01-31T23:59:59", "Crypto", "50"),
                row("2026-10-23T14:00:00", "ETF", "25")),
                AnalystPeriod.YEAR, AnalystPeriod.YEAR.startAt(now), now);
        assertEquals(10, points.size());
        assertEquals(new BigDecimal("150"), points.getFirst().tradeVolume());
        assertEquals(BigDecimal.ZERO, points.get(1).tradeVolume());
        assertEquals(LocalDateTime.parse("2026-10-01T00:00:00"), points.getLast().timestamp());
        assertEquals(new BigDecimal("25"), points.getLast().tradeVolume());
        assertEquals(new BigDecimal("175"), points.stream().map(point -> point.tradeVolume())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void emptyPeriodsAndFirstIntervalHaveZeroValuesWithoutFabricatedHistory() {
        for (var period : AnalystPeriod.values()) {
            if (period == AnalystPeriod.ALL_TIME) continue;
            var start = period.startAt(now);
            var points = AnalystService.buildActivity(List.of(), period, start, start);
            assertEquals(1, points.size());
            assertEquals(start, points.getFirst().timestamp());
            assertEquals(BigDecimal.ZERO, points.getFirst().tradeVolume());
            assertTrue(points.getFirst().instrumentVolumes().isEmpty());
        }

    }

    @Test
    void allTimeUsesMonthlyBucketsFromFirstRecordedOrderAcrossYears() {
        var points = AnalystService.buildActivity(List.of(
                row("2024-12-31T10:00:00", "Stock", "100"),
                row("2026-10-23T12:00:00", "ETF", "25")),
                AnalystPeriod.ALL_TIME, null, now);
        assertEquals(23, points.size());
        assertEquals(LocalDateTime.parse("2024-12-01T00:00:00"), points.getFirst().timestamp());
        assertEquals(LocalDateTime.parse("2026-10-01T00:00:00"), points.getLast().timestamp());
        assertEquals(BigDecimal.ZERO, points.get(1).tradeVolume());
        assertEquals(new BigDecimal("125"), points.stream().map(point -> point.tradeVolume())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void allTimeWithoutOrdersShowsOnlyCurrentMonthWithZeroVolume() {
        var points = AnalystService.buildActivity(List.of(), AnalystPeriod.ALL_TIME, null, now);
        assertEquals(1, points.size());
        assertEquals(LocalDateTime.parse("2026-10-01T00:00:00"), points.getFirst().timestamp());
        assertEquals(BigDecimal.ZERO, points.getFirst().tradeVolume());
    }

    @Test
    void leapMonthAndCrossYearWeekKeepContinuousCalendarDates() {
        var leapDay = LocalDateTime.parse("2024-02-29T23:59:59");
        var month = AnalystService.buildActivity(List.of(), AnalystPeriod.MONTH,
                AnalystPeriod.MONTH.startAt(leapDay), leapDay);
        assertEquals(29, month.size());
        assertEquals(leapDay.toLocalDate().atStartOfDay(), month.getLast().timestamp());
        var newYear = LocalDateTime.parse("2026-01-01T12:00:00");
        var week = AnalystService.buildActivity(List.of(), AnalystPeriod.WEEK,
                AnalystPeriod.WEEK.startAt(newYear), newYear);
        assertEquals(4, week.size());
        assertEquals(LocalDateTime.parse("2025-12-29T00:00:00"), week.getFirst().timestamp());
        assertEquals(newYear.toLocalDate().atStartOfDay(), week.getLast().timestamp());
    }
}
