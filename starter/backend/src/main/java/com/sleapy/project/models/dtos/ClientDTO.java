package com.sleapy.project.models.dtos;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for client information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class ClientDTO {

    private Long id; // Unique client identifier
    private String email; // Email address (login credential)
    private String username; // Display username
    private double cashBalance; // Available cash balance
    private String phoneNumber; // Contact phone number
    private String ssnLast4; // Last 4 digits of SSN
    private String fullAddress; // Residential address
    private String birthDate; // Birth date (YYYY-MM-DD format)
    private ArrayList<HoldingDTO> holdings; // List of all holdings owned
}
