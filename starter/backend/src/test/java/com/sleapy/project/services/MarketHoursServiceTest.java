package com.sleapy.project.services;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketHoursServiceTest {
    @Test
    void convertsUtcToEasternDuringDaylightSavingTime() {
        MarketHoursService service = serviceAt("2026-07-06T14:00:00Z");

        assertTrue(service.isMarketOpen());
    }

    @Test
    void rejectsBeforeOpenAtEasternTime() {
        MarketHoursService service = serviceAt("2026-01-05T14:29:59Z");

        assertFalse(service.isMarketOpen());
    }

    @Test
    void rejectsAtMarketClose() {
        MarketHoursService service = serviceAt("2026-01-05T21:00:00Z");

        assertFalse(service.isMarketOpen());
    }

    @Test
    void rejectsWeekends() {
        MarketHoursService service = serviceAt("2026-01-10T15:00:00Z");

        assertFalse(service.isMarketOpen());
    }

    private MarketHoursService serviceAt(String instant) {
        return new MarketHoursService(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}