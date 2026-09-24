package com.sleapy.project.repositories;

import com.sleapy.project.models.entities.ClientEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis mapper interface for ClientEntity.
 * 
 * Defines database operations for the clients table.
 * SQL implementation is in ClientMapper.xml
 */
@Mapper
public interface ClientMapper {
    
    /**
     * Find a client by ID.
     * @param id the client ID
     * @return Optional containing the client, or empty if not found
     */
    Optional<ClientEntity> findById(Long id);
    
    /**
     * Find a client by email.
     * @param email the client email
     * @return Optional containing the client, or empty if not found
     */
    Optional<ClientEntity> findByEmail(String email);
    
    /**
     * Retrieve all clients.
     * @return list of all clients
     */
    List<ClientEntity> findAll();
    
    /**
     * Insert a new client.
     * @param client the client entity to insert
     */
    void save(ClientEntity client);
    
    /**
     * Update an existing client.
     * @param client the client entity with updated values
     */
    void update(ClientEntity client);
    
    /**
     * Delete a client by ID.
     * @param id the client ID
     */
    void deleteById(Long id);
    
    /**
     * Count total number of clients.
     * @return total count
     */
    long count();
}