package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.InstrumentTypeEntity;
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
    Optional<InstrumentTypeEntity> findById(Long id);

    /**
     * Get all instrument types.
     * @return List of all InstrumentType objects
     */
    List<InstrumentTypeEntity> findAll();

    /**
     * Save (insert) a new instrument type.
     * @param instrumentType the InstrumentType to save
     */
    void save(InstrumentTypeEntity instrumentType);

    /**
     * Update an existing instrument type.
     * @param instrumentType the InstrumentType to update
     */
    void update(InstrumentTypeEntity instrumentType);

    /**
     * Delete an instrument type by ID.
     * @param id the instrument type ID
     */
    void deleteById(Long id);
}
