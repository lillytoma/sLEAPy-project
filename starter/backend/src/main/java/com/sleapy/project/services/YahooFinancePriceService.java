package com.sleapy.project.services;

import com.sleapy.project.models.dtos.PriceUpdateDTO;
import com.sleapy.project.models.dtos.YahooSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;

/**
 * Price provider backed by a small Python service (starter/backend/etl/price_service.py)
 * that wraps yfinance. Keeps live market data fetching out of the JVM so we can reuse the
 * yfinance library directly instead of a Java port of it.
 */
@Service
@Slf4j
public class YahooFinancePriceService implements PriceService {

    private final RestClient restClient;

    public YahooFinancePriceService(RestClient.Builder restClientBuilder,
                                     @Value("${price-service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        PriceUpdateDTO quote = fetchQuote(symbol);
        return quote != null ? quote.getCurrentPrice() : null;
    }

    @Override
    public BigDecimal[] getPricesForSymbols(String... symbols) {
        Map<String, PriceUpdateDTO> quotes = getQuotes(List.of(symbols));
        return Stream.of(symbols)
                .map(symbol -> {
                    PriceUpdateDTO quote = quotes.get(symbol.trim().toUpperCase());
                    return quote != null ? quote.getCurrentPrice() : null;
                })
                .toArray(BigDecimal[]::new);
    }

    @Override
    public Map<String, PriceUpdateDTO> getQuotes(List<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }

        String joined = symbols.stream()
            .map(String::trim)
            .filter(symbol -> !symbol.isEmpty())
            .map(String::toUpperCase)
            .distinct()
            .collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, PriceUpdateDTO> quotes = restClient.get()
                    .uri("/quotes?symbols={symbols}", joined)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, PriceUpdateDTO>>() {
                    });
            return quotes != null ? quotes : Map.of();
        } catch (RestClientException e) {
            log.error("Failed to fetch quotes from price service for [{}]: {}", joined, e.getMessage());
            return Map.of();
        }
    }

    @Override
    public List<YahooSearchResultDTO> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<YahooSearchResultDTO> results = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("query", query.trim())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<YahooSearchResultDTO>>() {});
            return results != null ? results : List.of();
        } catch (RestClientException e) {
            log.error("Failed to search Yahoo Finance for [{}]: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private PriceUpdateDTO fetchQuote(String symbol) {
        try {
            return restClient.get()
                    .uri("/quote/{symbol}", symbol.trim().toUpperCase())
                    .retrieve()
                    .body(PriceUpdateDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch quote for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
}
