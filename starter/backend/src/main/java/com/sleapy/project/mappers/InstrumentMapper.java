package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.InstrumentEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
import java.util.List;

/**
 * MyBatis Mapper interface for InstrumentEntity operations.
 * Provides database access methods for financial instruments.
 */
@Mapper
public interface InstrumentMapper {
    /**
     * Find an instrument by its ID.
     * @param id the instrument ID
     * @return Optional containing the InstrumentEntity if found
     */
    Optional<InstrumentEntity> findById(Long id);

    /**
     * Find an instrument by symbol (e.g., AAPL, MSFT).
     * @param symbol the instrument symbol
     * @return Optional containing the InstrumentEntity if found
     */
    Optional<InstrumentEntity> findBySymbol(String symbol);

    /**
     * Get all instruments.
     * @return List of all InstrumentEntity objects
     */
    List<InstrumentEntity> findAll();

    /**
     * Save (insert) a new instrument.
     * @param instrument the InstrumentEntity to save
     */
    void save(InstrumentEntity instrument);

    /**
     * Update an existing instrument.
     * @param instrument the InstrumentEntity to update
     */
    void update(InstrumentEntity instrument);

    /**
     * Delete an instrument by ID.
     * @param id the instrument ID
     */
    void deleteById(Long id);

    /**
     * Get count of all instruments.
     * @return total number of instruments
     */
    int count();
}
