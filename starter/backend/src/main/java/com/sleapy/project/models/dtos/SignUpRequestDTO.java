package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SignUpRequestDTO{
    
    private String email; //user email
    private String password; //users password
    private String address; //client address
    private String phoneNumber; 
    private String ssn; //social

}