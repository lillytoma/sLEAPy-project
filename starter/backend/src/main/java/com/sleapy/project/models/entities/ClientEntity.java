package com.sleapy.project.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientEntity {
    
    private Long id; // Primary key
    private String email; // Unique email used for authentication
    private double cashBalance; // client's account balance
    private String passwordHash; // Bcrypt hashed password
    private String address; // Residential address
    private String phoneNumber; // Contact phone number
    private String ssn; // Social security number (encrypted)
    private ClientStatus clientStatus; // foreign key to ClientStatus entity

}
