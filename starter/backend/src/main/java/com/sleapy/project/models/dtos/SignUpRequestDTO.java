package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    
    private String email;        // user email
    private String password;     // user's password
    private String address;      // client address
    private String phoneNumber;  // client phone number
    private String ssn;          // social security number
}
