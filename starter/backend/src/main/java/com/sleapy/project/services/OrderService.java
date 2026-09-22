package com.sleapy.project.services;


import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderDTO> getTransactionsPerClient(Long clientId){
        List<OrderEntity> entities = orderRepository.findByClient_IdOrderByTimePurchasedAsc(clientId);
        System.out.println(entities.size());
        if(entities.isEmpty()){
            throw new NoSuchElementException("Could not find transactions for client id " + clientId);
        }

        return entities.stream().map(OrderDTO::new).toList();
    }

}
