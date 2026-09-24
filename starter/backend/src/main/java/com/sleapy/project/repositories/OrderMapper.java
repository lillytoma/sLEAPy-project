package com.sleapy.project.repositories;

import com.sleapy.project.models.entities.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * MyBatis mapper interface for OrderEntity.
 * 
 * Defines database operations for the orders table with related joins
 * to clients, instruments, and order status.
 * SQL implementation is in OrderMapper.xml
 */
@Mapper
public interface OrderMapper {
    
    /**
     * Find an order by ID.
     * @param id the order ID
     * @return the order entity with related client, instrument, and status, or null if not found
     */
    OrderEntity findById(Long id);
    
    /**
     * Retrieve all orders.
     * @return list of all orders with related entities loaded
     */
    List<OrderEntity> findAll();
    
    /**
     * Find all orders for a specific client, ordered by purchase time (ascending).
     * 
     * This replaces the JPA nested property query with explicit SQL JOIN.
     * 
     * @param clientId the client ID to search by
     * @return list of orders for the client, ordered by time_purchased ASC
     */
    List<OrderEntity> findByClient_IdOrderByTimeOfPurchaseAsc(Long clientId);
    
    /**
     * Insert a new order.
     * @param order the order entity to insert
     */
    void save(OrderEntity order);
    
    /**
     * Update an existing order.
     * @param order the order entity with updated values
     */
    void update(OrderEntity order);
    
    /**
     * Delete an order by ID.
     * @param id the order ID
     */
    void deleteById(Long id);
}
