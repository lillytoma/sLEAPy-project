package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    
    private String email; // User's email (unique identifier)
    private String password; // User's password (to be verified against hash)

}
