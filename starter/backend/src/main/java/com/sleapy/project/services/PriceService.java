package com.sleapy.project.services;

import com.sleapy.project.models.dtos.PriceUpdateDTO;
import com.sleapy.project.models.dtos.YahooSearchResultDTO;
import com.sleapy.project.models.dtos.PriceHistoryPointDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for fetching current stock prices.
 * Backed by a small Python service that wraps yfinance (see starter/backend/etl/price_service.py).
 *
 * This allows decoupling of price fetching from trading logic,
 * making it easy to swap price providers later.
 */

public interface PriceService {

    /**
     * Fetch the current price for a stock symbol.
     *
     * @param symbol the stock symbol (e.g., "AAPL", "MSFT")
     * @return BigDecimal representing the current price per share, or null if it could not be fetched
     */
    BigDecimal getCurrentPrice(String symbol);

    /**
     * Fetch multiple prices at once (batch operation for efficiency).
     * Useful when trading multiple instruments.
     *
     * @param symbols array of stock symbols
     * @return array of BigDecimals in the same order as input symbols; entries are null where a price could not be fetched
     */
    BigDecimal[] getPricesForSymbols(String... symbols);

    /**
     * Fetch full quotes (current/high/low price) for a batch of symbols.
     * Used by the scheduled refresh job to populate the market_prices cache.
     *
     * @param symbols the stock symbols to fetch quotes for
     * @return map of symbol -> PriceUpdateDTO; symbols that could not be fetched are omitted
     */
    Map<String, PriceUpdateDTO> getQuotes(List<String> symbols);

    List<YahooSearchResultDTO> search(String query);

    List<PriceHistoryPointDTO> getPriceHistory(String symbol, String period);
}
