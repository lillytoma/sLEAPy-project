package com.sleapy.project.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email; // User's email (unique identifier)

    @NotBlank(message = "password is required")
    private String password; // User's password (to be verified against hash)

}
