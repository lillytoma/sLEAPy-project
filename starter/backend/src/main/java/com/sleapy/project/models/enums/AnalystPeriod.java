package com.sleapy.project.models.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public enum AnalystPeriod {
    DAY, WEEK, MONTH, YEAR;

    public LocalDateTime startAt(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        return switch (this) {
            case DAY -> date.atStartOfDay();
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTH -> date.withDayOfMonth(1).atStartOfDay();
            case YEAR -> date.withDayOfYear(1).atStartOfDay();
        };
    }
}
