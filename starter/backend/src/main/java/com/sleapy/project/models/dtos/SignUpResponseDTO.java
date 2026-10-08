package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseDTO{
    
    private String message;    // Success/status message
    private Long id;           // User unique ID
    private String jwtToken;   // JWT token

}