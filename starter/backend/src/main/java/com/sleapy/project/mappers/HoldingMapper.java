package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.HoldingEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
import java.util.List;

/**
 * MyBatis Mapper interface for HoldingEntity operations.
 * Provides database access methods for client holdings.
 */
@Mapper
public interface HoldingMapper {
    /**
     * Find a holding by its ID.
     * @param id the holding ID
     * @return Optional containing the HoldingEntity if found
     */
    Optional<HoldingEntity> findById(Long id);

    /**
     * Find all holdings for a specific client.
     * @param clientId the client ID
     * @return List of HoldingEntity objects for the client
     */
    List<HoldingEntity> findByClientId(Long clientId);

    /**
     * Find holdings for a specific client and instrument.
     * @param clientId the client ID
     * @param instrumentId the instrument ID
     * @return Optional containing the HoldingEntity if found
     */
    Optional<HoldingEntity> findByClientIdAndInstrumentId(Long clientId, Long instrumentId);

    /**
     * Get all holdings.
     * @return List of all HoldingEntity objects
     */
    List<HoldingEntity> findAll();

    /**
     * Save (insert) a new holding.
     * @param holding the HoldingEntity to save
     */
    void save(HoldingEntity holding);

    /**
     * Update an existing holding.
     * @param holding the HoldingEntity to update
     */
    void update(HoldingEntity holding);

    /**
     * Delete a holding by ID.
     * @param id the holding ID
     */
    void deleteById(Long id);

    /**
     * Get count of all holdings.
     * @return total number of holdings
     */
    int count();
}
