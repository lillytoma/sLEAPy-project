package com.sleapy.project.models.entities;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sleapy.project.models.dtos.SignUpRequestDTO;

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

    ClientEntity(SignUpRequestDTO dto){
        this.email = dto.getEmail();
        this.address = dto.getAddress();
        this.phoneNumber = dto.getPhoneNumber();
        this.ssn = dto.getSsn();
        this.cashBalance = 0.0;

        var encoder = new BCryptPasswordEncoder();
        this.passwordHash = encoder.encode(dto.getPassword());
        this.clientStatus = new ClientStatus((long)0, "ACTIVE" );
    }

}
