package com.sleapy.project.repositories;

import com.sleapy.project.models.entities.ClientStatus;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
import java.util.List;

/**
 * MyBatis Mapper interface for ClientStatus (lookup table).
 * Provides database access methods for client status records.
 */
@Mapper
public interface ClientStatusMapper {
    /**
     * Find a client status by ID.
     * @param id the status ID
     * @return Optional containing the ClientStatus if found
     */
    Optional<ClientStatus> findById(Long id);

    /**
     * Get all client statuses.
     * @return List of all ClientStatus objects
     */
    List<ClientStatus> findAll();

    /**
     * Save (insert) a new client status.
     * @param status the ClientStatus to save
     */
    void save(ClientStatus status);

    /**
     * Update an existing client status.
     * @param status the ClientStatus to update
     */
    void update(ClientStatus status);

    /**
     * Delete a client status by ID.
     * @param id the status ID
     */
    void deleteById(Long id);
}
