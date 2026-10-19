package com.sleapy.project.services;

import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.MarketPriceMapper;
import com.sleapy.project.models.dtos.InstrumentWithPriceDTO;
import com.sleapy.project.models.dtos.PriceUpdateDTO;
import com.sleapy.project.models.dtos.YahooSearchResultDTO;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.InstrumentType;
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
    private final PriceService priceService;

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
     * Search instruments by ticker or company name (case-insensitive, partial match).
     * @param query search term (symbol or company name)
     * @param field either {@code ticker} or {@code company}
     * @return List of matching InstrumentWithPriceDTO objects
     */
    public List<InstrumentWithPriceDTO> searchInstruments(String query, String field) {
        List<InstrumentEntity> allInstruments = instrumentMapper.findAll();
        String lowerQuery = query.toLowerCase().trim();

        List<InstrumentWithPriceDTO> localResults = allInstruments.stream()
                .filter(instrument ->
                        field.equals("ticker")
                                ? instrument.getSymbol().toLowerCase().contains(lowerQuery)
                                : instrument.getSymbolName().toLowerCase().contains(lowerQuery)
                )
                .map(instrument -> {
                    Optional<MarketPriceEntity> price = marketPriceMapper.findBySymbol(instrument.getSymbol());
                    return new InstrumentWithPriceDTO(instrument, price.orElse(null));
                })
                .collect(Collectors.toList());

        // Use the local catalog first. Only query Yahoo when the database has no
        // matches, avoiding an external request for every search of known symbols.
        if (!localResults.isEmpty()) {
            return localResults;
        }

        var yahooResults = priceService.search(query);
        yahooResults = yahooResults.stream()
                .filter(result -> field.equals("ticker")
                        ? result.getSymbol() != null && result.getSymbol().toLowerCase().contains(lowerQuery)
                        : result.getSymbolName() != null && result.getSymbolName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());

        // Yahoo's search response already contains regularMarketPrice when
        // available. Do not fetch every search result again through /quotes:
        // that creates a request fan-out and quickly triggers Yahoo throttling.
        yahooResults.stream()
                .filter(result -> result.getSymbol() != null)
                .map(InstrumentWithPriceDTO::new)
                .forEach(localResults::add);

        return localResults;
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

    public InstrumentWithPriceDTO importFromYahoo(YahooSearchResultDTO request) {
        if (request == null || request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("A Yahoo Finance symbol is required");
        }

        String symbol = request.getSymbol().trim().toUpperCase();
        Optional<InstrumentEntity> existing = instrumentMapper.findBySymbol(symbol);
        if (existing.isPresent()) {
            refreshImportedPrice(symbol);
            return getInstrumentBySymbol(symbol).orElseThrow();
        }

        PriceUpdateDTO quote = priceService.getQuotes(List.of(symbol)).get(symbol);
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setSymbol(symbol);
        instrument.setSymbolName(request.getSymbolName() == null || request.getSymbolName().isBlank()
                ? symbol : request.getSymbolName());
        instrument.setCurrentPrice(quote != null ? quote.getCurrentPrice() : request.getCurrentPrice());

        InstrumentType stockType = new InstrumentType();
        stockType.setInstrumentType_id(1L);
        stockType.setName("Stock");
        instrument.setInstrumentType(stockType);
        instrumentMapper.save(instrument);

        if (quote != null) {
            updateMarketPrice(quote.getSymbol(), new MarketPriceEntity(
                    quote.getSymbol(), quote.getCurrentPrice(), quote.getHighPrice(),
                    quote.getLowPrice(), quote.getLastUpdated()));
        }
        return new InstrumentWithPriceDTO(instrument, quote == null ? null : new MarketPriceEntity(
                symbol, quote.getCurrentPrice(), quote.getHighPrice(), quote.getLowPrice(), quote.getLastUpdated()));
    }

    private void refreshImportedPrice(String symbol) {
        PriceUpdateDTO quote = priceService.getQuotes(List.of(symbol)).get(symbol);
        if (quote != null) {
            updateMarketPrice(symbol, new MarketPriceEntity(
                    symbol, quote.getCurrentPrice(), quote.getHighPrice(),
                    quote.getLowPrice(), quote.getLastUpdated()));
        }
    }

    /**
     * Refresh market prices from the live price service (yfinance-backed).
     * Fetches the latest quote for every known instrument, upserts it into the
     * market_prices cache table, and returns the data for WebSocket broadcast.
     * Called periodically by @Scheduled task every 5 seconds.
     *
     * @return Map of symbol -> PriceUpdateDTO for broadcasting via WebSocket
     */
    public Map<String, PriceUpdateDTO> refreshPrices() {
        List<String> symbols = instrumentMapper.findAll().stream()
                .map(InstrumentEntity::getSymbol)
                .collect(Collectors.toList());

        if (symbols.isEmpty()) {
            return Map.of();
        }

        Map<String, PriceUpdateDTO> quotes = priceService.getQuotes(symbols);

        quotes.values().forEach(quote -> updateMarketPrice(quote.getSymbol(), new MarketPriceEntity(
                quote.getSymbol(),
                quote.getCurrentPrice(),
                quote.getHighPrice(),
                quote.getLowPrice(),
                quote.getLastUpdated()
        )));

        return quotes;
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
