package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for login requests.
 * Captures user credentials for authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    
    private String email; // User's email (unique identifier)
    private String password; // User's password (to be verified against hash)

}
