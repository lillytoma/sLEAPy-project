package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.ClientEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * MyBatis mapper interface for ClientEntity.
 * 
 * Defines database operations for the clients table.
 * SQL implementation is in ClientMapper.xml
 */
@Mapper
public interface ClientMapper {
    @org.apache.ibatis.annotations.Select("SELECT Client_ID FROM Clients WHERE Client_ID = #{id} FOR UPDATE")
    Long lockForTrading(Long id);
    
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
     * @param client the client entity to insert (will be modified with auto-generated ID)
     */
    void save(ClientEntity client);
    
    /**
     * Update an existing client.
     * @param client the client entity with updated values
     */
    void update(ClientEntity client);

    void addCash(@org.apache.ibatis.annotations.Param("id") Long id,
                 @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);

    int withdrawCash(@org.apache.ibatis.annotations.Param("id") Long id,
                     @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);

    void updateStatus(@org.apache.ibatis.annotations.Param("id") Long id,
                      @org.apache.ibatis.annotations.Param("statusId") Long statusId);

    void deleteHoldingsByClientId(Long clientId);

    void deleteOrdersByClientId(Long clientId);
    
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