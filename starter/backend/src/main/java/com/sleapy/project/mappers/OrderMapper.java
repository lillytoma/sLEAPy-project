package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.OrderEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

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
    @Select("""
        SELECT o.Order_ID FROM Orders o JOIN Order_Status s ON s.OrderStatus_ID = o.Status
        WHERE s.OrderStatus_Name = 'Queued' ORDER BY o.Order_ID LIMIT 100
        """)
    List<Long> findQueuedIds();

    @Select("""
        SELECT Order_ID FROM Orders
        WHERE Order_ID = #{id} AND Status IN (
            SELECT OrderStatus_ID FROM Order_Status WHERE OrderStatus_Name = 'Queued'
        )
        FOR UPDATE SKIP LOCKED
        """)
    Long lockQueued(Long id);
    
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
     * This method is defined with annotations to avoid runtime XML statement lookup mismatches.
     *
     * @param clientId the client ID to search by
     * @return list of orders for the client, ordered by time_purchased ASC
     */
    @Select("""
        SELECT o.Order_ID,
               o.Quantity,
               o.Time_purchased,
               o.Time_filled,
               o.Purchase_Price,
               o.trade_type,
               o.Status AS OrderStatus_ID,
               os.OrderStatus_Name,
               c.Client_ID,
               c.email AS client_email,
               c.cash_balance AS client_cash_balance,
               c.password_hash AS client_password_hash,
               c.street_address AS client_street_address,
               c.phone_number AS client_phone_number,
               c.ssn AS client_ssn,
               i.Instrument_ID,
               i.Symbol AS instrument_symbol,
               i.Symbol_Name AS instrument_symbol_name
        FROM Orders o
        LEFT JOIN Order_Status os ON o.Status = os.OrderStatus_ID
        LEFT JOIN Clients c ON o.Client_ID = c.Client_ID
        LEFT JOIN Instruments i ON o.Instrument_ID = i.Instrument_ID
        WHERE c.Client_ID = #{clientId}
        ORDER BY o.Time_purchased ASC
        """)
    @Results({
        @Result(column = "Order_ID", property = "id", id = true),
        @Result(column = "Quantity", property = "quantity"),
        @Result(column = "Time_purchased", property = "timeOfPurchase"),
        @Result(column = "Time_filled", property = "timeFilled"),
        @Result(column = "Purchase_Price", property = "purchasePrice"),
        @Result(column = "trade_type", property = "tradeType"),
        @Result(column = "OrderStatus_ID", property = "status.id"),
        @Result(column = "OrderStatus_Name", property = "status.name"),
        @Result(column = "Client_ID", property = "client.id"),
        @Result(column = "client_email", property = "client.email"),
        @Result(column = "client_cash_balance", property = "client.cashBalance"),
        @Result(column = "client_password_hash", property = "client.passwordHash"),
        @Result(column = "client_street_address", property = "client.streetAddress"),
        @Result(column = "client_phone_number", property = "client.phoneNumber"),
        @Result(column = "client_ssn", property = "client.ssn"),
        @Result(column = "Instrument_ID", property = "instrument.id"),
        @Result(column = "instrument_symbol", property = "instrument.symbol"),
        @Result(column = "instrument_symbol_name", property = "instrument.symbolName")
    })
    List<OrderEntity> findTransactionsByClientId(@Param("clientId") Long clientId);


    /**
     * 
     * @param clientId The client whose orders to search by
     * @param offset how many rows to skip before returning list
     * @param pageSize how large the page should be
     * @return List of entities queried by page
     */
        List<OrderEntity> findTransactionsByClientAndPage(
            @Param("clientId") Long clientId,
            @Param("offset") Long offset,
            @Param("pageSize") Long pageSize
        );
    
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
