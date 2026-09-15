package com.sleapy.project.controllers;

import com.sleapy.project.models.OrderDTO;
import com.sleapy.project.services.OrderService;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    //fetch and render transaction list
    @GetMapping("/{clientId}")
    public ResponseEntity<List<OrderDTO>> getTransactions(@PathVariable Long clientId) {
        try{
            List<OrderDTO> transactionList = orderService.getTransactionsPerClient(clientId);
            return  ResponseEntity.ok(transactionList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }


    }

}
