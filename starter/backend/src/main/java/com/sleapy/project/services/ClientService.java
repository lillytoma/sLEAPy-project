package com.sleapy.project.services;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.ClientStatus;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.models.dtos.SignUpRequestDTO;
import com.sleapy.project.validators.ClientValidator;
import com.sleapy.project.exceptions.InvalidEmailFormatException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import org.springframework.web.bind.annotation.CrossOrigin;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ClientService {

    private final ClientValidator clientValidator;

    private final ClientMapper clientMapper;

    public double getCashBalance(Long clientId) {
        
        ClientEntity client = clientMapper.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));  
        return client.getCashBalance(); // Return the actual current balance from the client entity
    }
    
//Entry point for checking a login attempt. Given an email and the plaintext password. It returns a bool answering whether the credentials exist in the db
    public boolean checkCredentials(String email, String rawPassword) { 
        //Loos up the database for a client with the following email and stores it in clientInfo
        Optional<ClientEntity> clientInfo = clientMapper.findByEmail(email);

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

    public boolean isUniqueEmail(String email) throws InvalidEmailFormatException {
        clientValidator.validateEmail(email);
        Optional<ClientEntity> clientInfo = clientMapper.findByEmail(email);
        if(!clientInfo.isPresent()){
            return false;
        }
        return true;
    }

    /**
     * Register a new client (signup flow)
     * @param request SignUpRequestDTO containing email, password, name, address, phone, ssn
     * @return the newly created ClientEntity with auto-generated ID
     */
    public ClientEntity signup(SignUpRequestDTO request) {
        // Hash password with BCrypt
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // Create new client entity
        ClientEntity newClient = new ClientEntity();
        newClient.setUsername(request.getUsername());
        newClient.setEmail(request.getEmail());
        newClient.setPasswordHash(hashedPassword);
        newClient.setFirstName(request.getFirstName());
        newClient.setLastName(request.getLastName());
        newClient.setDob(java.time.LocalDate.parse(request.getDob()));
        newClient.setStreetAddress(request.getStreetAddress());
        newClient.setCity(request.getCity());
        newClient.setStateName(request.getStateName());
        newClient.setZipCode(request.getZipCode());
        newClient.setRegion(request.getRegion());
        newClient.setPhoneNumber(request.getPhoneNumber());
        newClient.setSsn(request.getSsn());
        newClient.setCashBalance(0.0);  // Default balance for new users
        
        // Set active status (assuming 1 = ACTIVE in your database)
        ClientStatus activeStatus = new ClientStatus();
        activeStatus.setId(1L);
        newClient.setClientStatus(activeStatus);
        
        // Save to database (ID will be auto-generated and populated in newClient)
        clientMapper.save(newClient);
        return newClient;
    }
}