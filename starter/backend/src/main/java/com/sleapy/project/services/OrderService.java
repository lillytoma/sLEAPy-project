package com.sleapy.project.services;


import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private OrderRepository orderRepository;

    public List<OrderDTO> getTransactionsPerClient(Long clientId){
        List<OrderEntity> entities = orderRepository.findByClient_IdOrderByTimeOfPurchaseAsc(clientId);
        if(entities.isEmpty()){
            throw new RuntimeException("No transactions found for client id" + clientId);
        }

        return entities.stream().map(OrderDTO::new).toList();
    }

}
