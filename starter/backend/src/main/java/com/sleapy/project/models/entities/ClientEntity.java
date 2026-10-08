package com.sleapy.project.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ClientEntity {
    
    private Long id; // Primary key (client_id)
    private String username; // Unique username for login
    private String email; // Unique email used for authentication
    private double cashBalance; // client's account balance
    private String passwordHash; // Bcrypt hashed password
    private String firstName; // First name
    private String lastName; // Last name
    private LocalDate dob; // Date of birth
    private String phoneNumber; // Contact phone number
    private String ssn; // Social security number (encrypted)
    private String streetAddress; // Street address
    private String city; // City
    private String stateName; // State name
    private String zipCode; // Zip code
    private String region; // Region
    private ClientStatus clientStatus; // foreign key to ClientStatus entity
}
