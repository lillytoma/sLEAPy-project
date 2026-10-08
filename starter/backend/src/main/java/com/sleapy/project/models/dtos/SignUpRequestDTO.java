package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    
    private String username;     // unique username for login
    private String email;        // user email
    private String password;     // user's password
    private String firstName;    // client first name
    private String lastName;     // client last name
    private String dob;          // date of birth (YYYY-MM-DD)
    private String streetAddress; // street address
    private String city;         // city
    private String stateName;    // state name
    private String zipCode;      // zip code
    private String region;       // region
    private String phoneNumber;  // client phone number
    private String ssn;          // social security number
}
