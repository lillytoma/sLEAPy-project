package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.OrderDTO;
import com.sleapy.project.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    //fetch and render transaction list
    // test with: curl -iX GET localhost:8081/api/orders/?clientId=1
    @GetMapping(value = "/", params = {"clientId", "!page", "!pageSize"})
    public ResponseEntity<List<OrderDTO>> getTransactions(@RequestParam Long clientId) {
        List<OrderDTO> transactionList = orderService.getTransactionsPerClient(clientId);
        return ResponseEntity.ok(transactionList);
    }

    //test with curl -iX GET http://localhost:8081/api/orders?clientId=1&pageSize=10&page=1
    // 
    @GetMapping(value = "/", params = {"clientId", "page", "pageSize"})
    public ResponseEntity<List<OrderDTO>> getTransactionsByPage(@RequestParam Long clientId, @RequestParam Long page, @RequestParam Long pageSize){
        List<OrderDTO> transactionList = orderService.getTransactionsPerClientPaginated(clientId, page, pageSize);
        return ResponseEntity.ok(transactionList);
    }

}
