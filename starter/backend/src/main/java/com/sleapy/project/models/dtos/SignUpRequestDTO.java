package com.sleapy.project.models.dtos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    
    private String username;     // unique username for login
    private String firstname;    // first name
    private String lastname;     // last name
    private LocalDate dob;       // date of birth
    private String email;        // user email
    private String password;     // user's password
    private String phoneNumber;  // client phone number
    private String ssn;          // social security number
    private String street;       // street address
    private String city;         // city
    private String state;        // state name
    private String zip;          // zip code
    private String region;       // region
}
