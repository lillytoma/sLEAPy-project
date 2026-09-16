package com.sleapy.project.services;

import com.sleapy.project.models.ClientEntity;
import com.sleapy.project.repositories.ClientRepository;

import org.springframework.stereotype.Service;

@Service
public class ClientService {
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    private final ClientRepository clientRepository;

    public double getCurrentBalance(Long clientId) {
        
        ClientEntity client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));  
        return client.getCurrentBalance(); // Return the actual current balance from the client entity
    }
    
}
