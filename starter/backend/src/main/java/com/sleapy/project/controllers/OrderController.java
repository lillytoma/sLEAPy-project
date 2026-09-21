package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    //fetch and render transaction list
    // test with: curl -iX GET localhost:8081/api/orders/?clientId=1
    @GetMapping("/")
    public ResponseEntity<?> getTransactions(@RequestParam Long clientId) {
        try{
            List<OrderDTO> transactionList = orderService.getTransactionsPerClient(clientId);
            return  ResponseEntity.ok(transactionList);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }

    }

}
