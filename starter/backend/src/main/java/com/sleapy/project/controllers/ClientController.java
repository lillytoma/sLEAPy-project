package com.sleapy.project.controllers;



import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sleapy.project.services.ClientService;


@RestController
@RequestMapping("/api/clients")

public class ClientController {
    private final ClientService clientService;

    ClientController(ClientService cs){
        this.clientService = cs;
    }

    @GetMapping("/{clientId}/balance")
    public ResponseEntity<Double> getCashBalance(@PathVariable Long clientId) {

        double balance = clientService.getCashBalance(clientId);
        return new ResponseEntity<>(balance, HttpStatus.OK);
    }
  

   
}
