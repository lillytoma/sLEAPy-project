package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//This class represents a data transfer object (DTO) for login requests. It contains two fields: email and password, which are used to capture the user's login credentials when they attempt to authenticate with the system.
public class LoginRequestDTO {
    //The email field stores the user's email address, which is used as the unique identifier for authentication purposes.
    private String email;
    private String password;

}
