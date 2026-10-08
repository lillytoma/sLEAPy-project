package com.sleapy.project.services;

import java.math.BigDecimal;

/**
 * Service interface for fetching current stock prices.
 * Designed to be implemented with y-finance or other price data providers.
 * 
 * This allows decoupling of price fetching from trading logic,
 * making it easy to swap price providers later.
 */
public interface PriceService {
    
    /**
     * Fetch the current price for a stock symbol from y-finance or market data provider.
     * 
     * TODO: Implement with y-finance API
     * Example: https://github.com/sstrickx/yahoofinance-api
     * 
     * @param symbol the stock symbol (e.g., "AAPL", "MSFT")
     * @return BigDecimal representing the current price per share
     * @throws IllegalArgumentException if symbol is invalid or price cannot be fetched
     */
    BigDecimal getCurrentPrice(String symbol);
    
    /**
     * Fetch multiple prices at once (batch operation for efficiency).
     * Useful when trading multiple instruments.
     * 
     * TODO: Implement with y-finance batch API
     * 
     * @param symbols array of stock symbols
     * @return array of BigDecimals in the same order as input symbols
     * @throws IllegalArgumentException if any symbol is invalid
     */
    BigDecimal[] getPricesForSymbols(String... symbols);
}
