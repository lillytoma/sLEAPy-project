package com.sleapy.project.services;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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

        boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        return weekday && !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
}