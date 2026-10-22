package com.sleapy.project.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketHoursServiceTest {
    @ParameterizedTest
    @CsvSource({
        "2026-10-22T18:15:00Z,true",
        "2026-01-01T16:00:00Z,false",
        "2026-01-19T16:00:00Z,false",
        "2026-02-16T16:00:00Z,false",
        "2026-04-03T16:00:00Z,false",
        "2026-05-25T16:00:00Z,false",
        "2026-06-19T16:00:00Z,false",
        "2026-07-03T16:00:00Z,false",
        "2026-09-07T16:00:00Z,false",
        "2026-11-26T16:00:00Z,false",
        "2026-12-25T16:00:00Z,false",
        "2026-11-27T17:59:59Z,true",
        "2026-11-27T18:00:00Z,false",
        "2026-12-24T17:59:59Z,true",
        "2026-12-24T18:00:00Z,false",
        "2025-07-03T16:59:59Z,true",
        "2025-07-03T17:00:00Z,false",
        "2021-12-31T16:00:00Z,true",
        "2023-01-02T16:00:00Z,false",
        "2026-03-09T13:29:59Z,false",
        "2026-03-09T13:30:00Z,true"
    })
    void followsSharedNasdaqAndNyseCalendar(String instant, boolean expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, serviceAt(instant).isMarketOpen());
    }
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