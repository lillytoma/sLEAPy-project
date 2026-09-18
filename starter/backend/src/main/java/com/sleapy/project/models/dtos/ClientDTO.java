package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor 
public class ClientDTO {

    private final Long id;
    private final String email;
    private final String username;
    private final double cashBalance;
    private final String phoneNumber;
    private final String ssnLast4;
    private final String fullAddress;
    private final String birthDate; // YYYY-MM-DD

    // private final Portfolio portfolio;
   
}
