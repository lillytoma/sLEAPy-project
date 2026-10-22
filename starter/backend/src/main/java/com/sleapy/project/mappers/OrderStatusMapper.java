package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
import java.util.List;

/**
 * MyBatis Mapper interface for OrderStatus (lookup table).
 * Provides database access methods for order status records.
 */
@Mapper
public interface OrderStatusMapper {
    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO Order_Status (OrderStatus_Name)
        SELECT #{name} WHERE NOT EXISTS (
            SELECT 1 FROM Order_Status WHERE LOWER(OrderStatus_Name) = LOWER(#{name})
        )
        """)
    void ensureStatus(String name);

    @org.apache.ibatis.annotations.Select("""
        SELECT OrderStatus_ID AS id, OrderStatus_Name AS name FROM Order_Status
        WHERE LOWER(OrderStatus_Name) = LOWER(#{name}) ORDER BY OrderStatus_ID LIMIT 1
        """)
    Optional<OrderStatus> findByName(String name);
    /**
     * Find an order status by ID.
     * @param id the status ID
     * @return Optional containing the OrderStatus if found
     */
    Optional<OrderStatus> findById(Long id);

    /**
     * Get all order statuses.
     * @return List of all OrderStatus objects
     */
    List<OrderStatus> findAll();

    /**
     * Save (insert) a new order status.
     * @param status the OrderStatus to save
     */
    void save(OrderStatus status);

    /**
     * Update an existing order status.
     * @param status the OrderStatus to update
     */
    void update(OrderStatus status);

    /**
     * Delete an order status by ID.
     * @param id the status ID
     */
    void deleteById(Long id);
}
