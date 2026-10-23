package com.sleapy.project.services;

import com.sleapy.project.models.enums.AnalystPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalystPeriodTest {
    @Test
    void calendarPeriodsStartAtMidnight() {
        var now = LocalDateTime.parse("2026-10-23T13:41:42");
        assertEquals(LocalDateTime.parse("2026-10-23T00:00:00"), AnalystPeriod.DAY.startAt(now));
        assertEquals(LocalDateTime.parse("2026-10-19T00:00:00"), AnalystPeriod.WEEK.startAt(now));
        assertEquals(LocalDateTime.parse("2026-10-01T00:00:00"), AnalystPeriod.MONTH.startAt(now));
        assertEquals(LocalDateTime.parse("2026-01-01T00:00:00"), AnalystPeriod.YEAR.startAt(now));
    }

    @Test
    void mondaySundayAndYearRolloverUseCalendarBoundaries() {
        var monday = LocalDateTime.parse("2026-10-19T00:00:00");
        assertEquals(monday, AnalystPeriod.WEEK.startAt(monday));
        assertEquals(monday, AnalystPeriod.WEEK.startAt(monday.plusDays(6).plusHours(23)));
        assertEquals(LocalDateTime.parse("2025-12-29T00:00:00"),
                AnalystPeriod.WEEK.startAt(LocalDateTime.parse("2026-01-01T12:00:00")));
        assertEquals(LocalDateTime.parse("2024-02-01T00:00:00"),
                AnalystPeriod.MONTH.startAt(LocalDateTime.parse("2024-02-29T23:59:59")));
    }
}
