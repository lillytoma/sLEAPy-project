package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.models.dtos.AnalystActivityPointDTO;
import com.sleapy.project.models.dtos.AnalystOrderDirectionDTO;
import com.sleapy.project.models.dtos.AnalystPortfolioDTO;
import com.sleapy.project.models.dtos.AnalystPortfolioHistoryDTO;
import com.sleapy.project.models.enums.AnalystPeriod;
import com.sleapy.project.services.AnalystService;
import com.sleapy.project.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnalystControllerTest {
    @Mock private AnalystService analystService;
    @Mock private JwtService jwtService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AnalystController(analystService, jwtService)).build();
    }

    @Test
    void defaultsToDayAndAcceptsAllFourPeriodsWithClientFilter() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("Bearer analyst")).thenReturn(true);
        var summary = new AnalystSummaryDTO(BigDecimal.ZERO, 0, List.of(), null);
        when(analystService.getSummary(null, AnalystPeriod.DAY)).thenReturn(summary);
        mvc.perform(get("/api/analyst/summary").header("Authorization", "Bearer analyst"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalTrades").value(0));
        verify(analystService).getSummary(null, AnalystPeriod.DAY);
        for (var period : AnalystPeriod.values()) {
            when(analystService.getSummary(1L, period)).thenReturn(summary);
            mvc.perform(get("/api/analyst/summary").param("clientId", "1")
                            .param("period", period.name().toLowerCase(java.util.Locale.ROOT))
                            .header("Authorization", "Bearer analyst"))
                    .andExpect(status().isOk());
            verify(analystService).getSummary(1L, period);
        }
    }

    @Test
    void ownerEndpointRequiresAuthorizationAndValidatesFilters() throws Exception {
        mvc.perform(get("/api/analyst/instrument-owners").param("symbol", "AAPL"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(analystService);
        when(jwtService.verifyAnalystAuthorizationHeader("******")).thenReturn(true);
        mvc.perform(get("/api/analyst/instrument-owners").param("symbol", " ")
                        .header("Authorization", "******")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/analyst/instrument-owners").param("symbol", "AAPL").param("clientIds", "1,-2")
                        .header("Authorization", "******")).andExpect(status().isBadRequest());
        verifyNoInteractions(analystService);
    }

    @Test
    void ownerEndpointReturnsCurrentSharesWithDeduplicatedCustomerSelection() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("******")).thenReturn(true);
        var owner = new com.sleapy.project.models.dtos.AnalystInstrumentOwnerDTO();
        owner.setClientId(1L);
        owner.setUsername("customer");
        owner.setShares(new BigDecimal("12.5000"));
        when(analystService.getInstrumentOwners("AAPL", List.of(1L, 2L))).thenReturn(List.of(owner));
        mvc.perform(get("/api/analyst/instrument-owners").param("symbol", " AAPL ").param("clientIds", "1,2,1")
                        .header("Authorization", "******"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("customer"))
                .andExpect(jsonPath("$[0].shares").value(12.5));
        verify(analystService).getInstrumentOwners("AAPL", List.of(1L, 2L));
    }

    @Test
    void acceptsMultipleCustomersAndDeduplicatesLegacyAndNewFilters() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("******")).thenReturn(true);
        when(analystService.getSummaryForClients(List.of(1L, 2L), AnalystPeriod.MONTH))
                .thenReturn(new AnalystSummaryDTO(BigDecimal.ZERO, 0, List.of(), null));
        mvc.perform(get("/api/analyst/summary").param("clientIds", "1,2,1")
                        .param("clientId", "2").param("period", "MONTH")
                        .header("Authorization", "******"))
                .andExpect(status().isOk());
        verify(analystService).getSummaryForClients(List.of(1L, 2L), AnalystPeriod.MONTH);
    }

    @Test
    void rejectsInvalidCustomerListsWithoutQueryingData() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("******")).thenReturn(true);
        for (var ids : List.of("1,-2", "0", "not-a-number")) {
            mvc.perform(get("/api/analyst/summary").param("clientIds", ids)
                            .header("Authorization", "******"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(analystService);
    }

    @Test
    void rejectsUnknownPeriodWithoutQueryingSummary() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("Bearer analyst")).thenReturn(true);
        mvc.perform(get("/api/analyst/summary").param("period", "ALL")
                        .header("Authorization", "Bearer analyst"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Period must be DAY, WEEK, MONTH, or YEAR"));
        verifyNoInteractions(analystService);
    }

    @Test
    void summaryStillRequiresAnalystAuthorization() throws Exception {
        mvc.perform(get("/api/analyst/summary").param("period", "YEAR"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(analystService);
    }

    @Test
    void activitySerializesTimestampAndInstrumentVolumesForTheChart() throws Exception {
        when(jwtService.verifyAnalystAuthorizationHeader("******")).thenReturn(true);
        var summary = new AnalystSummaryDTO(new BigDecimal("150"), 2, List.of(), null);
        summary.setActivity(List.of(new AnalystActivityPointDTO(
                LocalDateTime.parse("2026-10-23T09:00:00"), new BigDecimal("150"),
                Map.of("Stock", new BigDecimal("100"), "ETF", new BigDecimal("50")))));
        var buy = new AnalystOrderDirectionDTO();
        buy.setDirection("BUY");
        buy.setInstrumentType("Stock");
        buy.setTotalTrades(2);
        buy.setTradeVolume(new BigDecimal("150"));
        summary.setOrderDirections(List.of(buy));
        var portfolio = new AnalystPortfolioDTO(LocalDateTime.parse("2026-10-23T09:00:00"), 1,
                new BigDecimal("100"), new BigDecimal("150"), new BigDecimal("250"), 0,
                Map.of("Stock", new BigDecimal("150")));
        summary.setPortfolioHistory(new AnalystPortfolioHistoryDTO(portfolio.snapshotAt(), portfolio, portfolio, true));
        when(analystService.getSummary(null, AnalystPeriod.DAY)).thenReturn(summary);
        mvc.perform(get("/api/analyst/summary").header("Authorization", "******"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity[0].timestamp").value("2026-10-23T09:00:00"))
                .andExpect(jsonPath("$.activity[0].tradeVolume").value(150))
                .andExpect(jsonPath("$.activity[0].instrumentVolumes.Stock").value(100))
                .andExpect(jsonPath("$.activity[0].instrumentVolumes.ETF").value(50))
                .andExpect(jsonPath("$.orderDirections[0].direction").value("BUY"))
                .andExpect(jsonPath("$.orderDirections[0].instrumentType").value("Stock"))
                .andExpect(jsonPath("$.orderDirections[0].totalTrades").value(2))
                .andExpect(jsonPath("$.orderDirections[0].tradeVolume").value(150))
                .andExpect(jsonPath("$.portfolioHistory.latest.snapshotAt").value("2026-10-23T09:00:00"))
                .andExpect(jsonPath("$.portfolioHistory.latest.netWorth").value(250))
                .andExpect(jsonPath("$.portfolioHistory.latest.instrumentValues.Stock").value(150))
                .andExpect(jsonPath("$.portfolioHistory.comparable").value(true));
    }
}
