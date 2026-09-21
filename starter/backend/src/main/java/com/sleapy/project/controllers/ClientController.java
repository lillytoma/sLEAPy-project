package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sleapy.project.services.ClientService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/clients")
@AllArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @GetMapping("/{clientId}/balance")
    public ResponseEntity<Double> getCashBalance(@PathVariable Long clientId) {

        double balance = clientService.getCashBalance(clientId);
        return new ResponseEntity<>(balance, HttpStatus.OK);
    }
  

   
}
