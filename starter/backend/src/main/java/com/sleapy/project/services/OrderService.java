package com.sleapy.project.services;


import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.validators.OrderValidator;

import com.sleapy.project.exceptions.InvalidOrderException;
import com.sleapy.project.mappers.OrderMapper;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class OrderService {
    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;

    public OrderService(OrderValidator orderValidator, OrderMapper orderMapper) {
        this.orderValidator = orderValidator;
        this.orderMapper = orderMapper;
    }


    public List<OrderDTO> getTransactionsPerClient(Long clientId){
        List<OrderEntity> entities = orderMapper.findTransactionsByClientId(clientId);
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(OrderDTO::new).toList();
    }

    public OrderEntity createOrderEntity(OrderEntity order) throws InvalidOrderException{
        orderValidator.validateOrder(order);
        orderMapper.save(order);
        return order;
    }

    public List<OrderDTO> getTransactionsPerClientPaginated(Long clientId, Long pageNum, Long pageSize) {
        if (pageNum == null || pageSize == null || pageNum < 1 || pageSize < 1) {
            return List.of();
        }

        // offset represents how many rows to skip before returning results
        // if Page 2 is records 11-20 is LIMIT 10, OFFSET 10. skip first 10 return next 10. 
        // (2 - 1) * 10 = 10 so offset is 10
        Long offset = (pageNum - 1) * pageSize;
        List<OrderEntity> entities = orderMapper.findTransactionsByClientAndPage(clientId, offset, pageSize);
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(OrderDTO::new).toList();
    }

}
