package com.sleapy.project.services;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

@Service
public class MarketHoursService {
    static final ZoneId US_EASTERN = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final Clock clock;

    public MarketHoursService() {
        this(Clock.systemUTC());
    }

    MarketHoursService(Clock clock) {
        this.clock = clock;
    }

    public boolean isMarketOpen() {
        return isMarketOpen(clock.instant());
    }

    boolean isMarketOpen(Instant instant) {
        ZonedDateTime easternTime = instant.atZone(US_EASTERN);
        DayOfWeek day = easternTime.getDayOfWeek();
        LocalTime time = easternTime.toLocalTime();
        LocalDate date = easternTime.toLocalDate();
        LocalTime close = isEarlyClose(date) ? LocalTime.of(13, 0) : MARKET_CLOSE;

        boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        return weekday && !holidays(date.getYear()).contains(date)
                && !time.isBefore(MARKET_OPEN) && time.isBefore(close);
    }

    private boolean isEarlyClose(LocalDate date) {
        return (date.getMonthValue() == 7 && date.getDayOfMonth() == 3)
                || (date.getMonthValue() == 12 && date.getDayOfMonth() == 24)
                || date.equals(LocalDate.of(date.getYear(), 11, 1)
                    .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)).plusDays(1));
    }

    private Set<LocalDate> holidays(int year) {
        LocalDate newYear = LocalDate.of(year, 1, 1);
        if (newYear.getDayOfWeek() == DayOfWeek.SUNDAY) newYear = newYear.plusDays(1);
        return Set.of(newYear,
                LocalDate.of(year, 1, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)),
                LocalDate.of(year, 2, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)),
                easter(year).minusDays(2),
                LocalDate.of(year, 5, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                observed(LocalDate.of(year, 6, 19)),
                observed(LocalDate.of(year, 7, 4)),
                LocalDate.of(year, 9, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)),
                LocalDate.of(year, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)),
                observed(LocalDate.of(year, 12, 25)));
    }

    private LocalDate observed(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.minusDays(1);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }

    private LocalDate easter(int year) {
        int a = year % 19, b = year / 100, c = year % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int value = h + l - 7 * m + 114;
        return LocalDate.of(year, value / 31, value % 31 + 1);
    }
}