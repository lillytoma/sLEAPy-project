package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.InstrumentType;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
import java.util.List;

/**
 * MyBatis Mapper interface for InstrumentType (lookup table).
 * Provides database access methods for instrument type records.
 */
@Mapper
public interface InstrumentTypeMapper {
    /**
     * Find an instrument type by ID.
     * @param id the instrument type ID
     * @return Optional containing the InstrumentType if found
     */
    Optional<InstrumentType> findById(Long id);

    /**
     * Get all instrument types.
     * @return List of all InstrumentType objects
     */
    List<InstrumentType> findAll();

    /**
     * Save (insert) a new instrument type.
     * @param instrumentType the InstrumentType to save
     */
    void save(InstrumentType instrumentType);

    /**
     * Update an existing instrument type.
     * @param instrumentType the InstrumentType to update
     */
    void update(InstrumentType instrumentType);

    /**
     * Delete an instrument type by ID.
     * @param id the instrument type ID
     */
    void deleteById(Long id);
}
