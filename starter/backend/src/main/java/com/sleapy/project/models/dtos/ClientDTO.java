package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class ClientDTO {

    private Long id;
    private String email;
    private String username;
    private double cashBalance;
    private String phoneNumber;
    private String ssnLast4;
    private String fullAddress;
    private String birthDate; // YYYY-MM-DD

    // private final Portfolio portfolio;
   
}
