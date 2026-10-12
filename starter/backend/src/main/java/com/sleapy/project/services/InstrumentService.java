package com.sleapy.project.services;

import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.MarketPriceMapper;
import com.sleapy.project.models.dtos.InstrumentWithPriceDTO;
import com.sleapy.project.models.dtos.PriceUpdateDTO;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.MarketPriceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing instruments and their real-time market prices.
 * Handles fetching, searching, and caching market data.
 */
@Service
@RequiredArgsConstructor
public class InstrumentService {
    private final InstrumentMapper instrumentMapper;
    private final MarketPriceMapper marketPriceMapper;

    /**
     * Get all instruments with their current market prices.
     * @return List of InstrumentWithPriceDTO objects
     */
    public List<InstrumentWithPriceDTO> getAllInstruments() {
        List<InstrumentEntity> instruments = instrumentMapper.findAll();
        return instruments.stream()
                .map(instrument -> {
                    Optional<MarketPriceEntity> price = marketPriceMapper.findBySymbol(instrument.getSymbol());
                    return new InstrumentWithPriceDTO(instrument, price.orElse(null));
                })
                .collect(Collectors.toList());
    }

    /**
     * Search instruments by symbol or name (case-insensitive, partial match).
     * @param query search term (symbol or company name)
     * @return List of matching InstrumentWithPriceDTO objects
     */
    public List<InstrumentWithPriceDTO> searchInstruments(String query) {
        List<InstrumentEntity> allInstruments = instrumentMapper.findAll();
        String lowerQuery = query.toLowerCase().trim();

        return allInstruments.stream()
                .filter(instrument ->
                        instrument.getSymbol().toLowerCase().contains(lowerQuery) ||
                        instrument.getSymbolName().toLowerCase().contains(lowerQuery)
                )
                .map(instrument -> {
                    Optional<MarketPriceEntity> price = marketPriceMapper.findBySymbol(instrument.getSymbol());
                    return new InstrumentWithPriceDTO(instrument, price.orElse(null));
                })
                .collect(Collectors.toList());
    }

    /**
     * Get price for a single instrument by symbol.
     * @param symbol instrument symbol (e.g., AAPL)
     * @return Optional containing InstrumentWithPriceDTO if found
     */
    public Optional<InstrumentWithPriceDTO> getInstrumentBySymbol(String symbol) {
        Optional<InstrumentEntity> instrument = instrumentMapper.findBySymbol(symbol);
        if (instrument.isEmpty()) {
            return Optional.empty();
        }

        Optional<MarketPriceEntity> price = marketPriceMapper.findBySymbol(symbol);
        return Optional.of(new InstrumentWithPriceDTO(instrument.get(), price.orElse(null)));
    }

    /**
     * Refresh market prices from external data source (Python ETL).
     * This method fetches latest prices and updates the market_prices cache table.
     * Called periodically by @Scheduled task every 5 seconds.
     *
     * @return Map of symbol -> PriceUpdateDTO for broadcasting via WebSocket
     */
    public Map<String, PriceUpdateDTO> refreshPrices() {
        // TODO: Call Python ETL service to fetch latest prices
        // Example implementation:
        // Map<String, Map<String, BigDecimal>> prices = pythonEtlClient.fetchPrices();
        
        // For now, retrieve existing prices from database
        List<MarketPriceEntity> currentPrices = marketPriceMapper.findAll();
        
        // Convert to PriceUpdateDTO for WebSocket broadcast
        return currentPrices.stream()
                .collect(Collectors.toMap(
                        MarketPriceEntity::getSymbol,
                        price -> new PriceUpdateDTO(
                                price.getSymbol(),
                                price.getCurrentPrice(),
                                price.getHighPrice(),
                                price.getLowPrice(),
                                price.getLastUpdated()
                        )
                ));
    }

    /**
     * Update a single market price (called by Python ETL or external service).
     * @param symbol instrument symbol
     * @param marketPrice updated MarketPriceEntity
     */
    public void updateMarketPrice(String symbol, MarketPriceEntity marketPrice) {
        marketPrice.setSymbol(symbol);
        marketPrice.setLastUpdated(LocalDateTime.now());
        
        if (marketPriceMapper.existsBySymbol(symbol)) {
            marketPriceMapper.update(marketPrice);
        } else {
            marketPriceMapper.saveOrUpdate(marketPrice);
        }
    }

    /**
     * Bulk update market prices (called by scheduled refresh job).
     * @param prices Map of symbol -> MarketPriceEntity
     */
    public void updateAllMarketPrices(Map<String, MarketPriceEntity> prices) {
        prices.forEach((symbol, price) -> {
            price.setSymbol(symbol);
            price.setLastUpdated(LocalDateTime.now());
            marketPriceMapper.saveOrUpdate(price);
        });
    }
}
