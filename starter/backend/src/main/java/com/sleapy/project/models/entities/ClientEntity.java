package com.sleapy.project.models.entities;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientEntity {
    
    private Long id; // Primary key
    private String username; // Unique username for login
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String streetAddress;
    private String city;
    private String stateName;
    private String zipCode;
    private String region;
    private String email; // Unique email used for authentication
    private double cashBalance; // client's account balance
    private String passwordHash; // Bcrypt hashed password
    private String phoneNumber; // Contact phone number
    private String ssn; // Social security number (encrypted)
    private ClientStatus clientStatus; // foreign key to ClientStatus entity
}
