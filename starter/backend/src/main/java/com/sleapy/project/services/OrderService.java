package com.sleapy.project.services;


import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.validators.OrderValidator;

import lombok.RequiredArgsConstructor;

import com.sleapy.project.exceptions.InvalidOrderException;
import com.sleapy.project.mappers.OrderMapper;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Collections;

@Service
@RequiredArgsConstructor 
public class OrderService {
    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;


    public List<OrderDTO> getTransactionsPerClient(Long clientId){
        List<OrderEntity> entities = orderMapper.findTransactionsByClientId(clientId);
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream().map(OrderDTO::new).toList();
    }

    public OrderEntity createOrderEntity(OrderEntity order) throws InvalidOrderException{
        orderValidator.validateOrder(order);
        orderMapper.save(order);
        return order;
    }

}
