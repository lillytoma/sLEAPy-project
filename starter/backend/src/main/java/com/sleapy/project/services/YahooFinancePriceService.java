package com.sleapy.project.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Placeholder price provider bean.
 *
 * TradeService already falls back to instrument.current_price when this service
 * cannot provide a live quote, so this keeps the app bootable until external
 * market pricing is implemented.
 */
@Service
public class YahooFinancePriceService implements PriceService {

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        throw new UnsupportedOperationException("Live market pricing is not implemented yet");
    }

    @Override
    public BigDecimal[] getPricesForSymbols(String... symbols) {
        throw new UnsupportedOperationException("Live market pricing is not implemented yet");
    }
}
