package com.sleapy.project.services;

import com.sleapy.project.models.ClientEntity;
import com.sleapy.project.repositories.ClientRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor 
public class ClientService {

    private ClientRepository clientRepository;

    public double getBalance(Long clientId) {
        
        ClientEntity client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));  
        return client.getBalance(); // Return the actual current balance from the client entity
    }
    
}
