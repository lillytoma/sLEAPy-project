package com.sleapy.project.services;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.sleapy.project.models.ClientEntity;
import com.sleapy.project.repositories.ClientRepository;

import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ClientService {
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    private final ClientRepository clientRepository;

    public double getBalance(Long clientId) {
        
        ClientEntity client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));  
        return client.getBalance(); // Return the actual current balance from the client entity
    }
    
//Entry point for checking a login attempt. Given an email and the plaintext password. It returns a bool answering whether the credentials exist in the db
    public boolean checkCredentials(String email, String rawPassword) { 
        //Loos up the database for a client with the following email and stores it in clientInfo
        Optional<ClientEntity> clientInfo = clientRepository.findByEmail(email);

        //checks if clientInfo is empty, if it is then -> return false, meaning the email was not found.
        if (clientInfo.isEmpty()) {
            return false; // Email not found
        }
        //We are unwrapping the clientInfo Optional to get the actual ClientEntity object. 
        ClientEntity client = clientInfo.get();
        //Checking if the raw password provided by the user matches the hashed password in the db (plain text -> hash compared to the -> exisiting hash)
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //checks the incoming hashed password against the stored hash and returns true if they match, (bool).
        return passwordEncoder.matches(rawPassword, client.getPasswordHash());
    }
}