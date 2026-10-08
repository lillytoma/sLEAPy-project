package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.MarketPriceEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper interface for MarketPriceEntity operations.
 * Provides database access methods for real-time market prices.
 */
@Mapper
public interface MarketPriceMapper {
    /**
     * Find market price by symbol.
     * @param symbol the instrument symbol (e.g., AAPL)
     * @return Optional containing the MarketPriceEntity if found
     */
    Optional<MarketPriceEntity> findBySymbol(String symbol);

    /**
     * Get all market prices.
     * @return List of all MarketPriceEntity objects
     */
    List<MarketPriceEntity> findAll();

    /**
     * Save or update market price (upsert).
     * @param marketPrice the MarketPriceEntity to save or update
     */
    void saveOrUpdate(MarketPriceEntity marketPrice);

    /**
     * Update an existing market price.
     * @param marketPrice the MarketPriceEntity to update
     */
    void update(MarketPriceEntity marketPrice);

    /**
     * Delete market price by symbol.
     * @param symbol the instrument symbol
     */
    void deleteBySymbol(String symbol);

    /**
     * Check if a market price exists for the given symbol.
     * @param symbol the instrument symbol
     * @return true if exists, false otherwise
     */
    boolean existsBySymbol(String symbol);
}
