package com.sleapy.project.models.dtos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    
    private String username;     // unique username for login
    private LocalDate dob;       // date of birth
    private String email;        // user email
    private String password;     // user's password
    private String firstName;    // client first name
    private String lastName;     // client last name
    private String streetAddress; // street address
    private String city;         // city
    private String stateName;    // state name
    private String zipCode;      // zip code
    private String region;       // region
}
