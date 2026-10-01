package com.sleapy.project.models.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseDTO{
    
    private String email; //user email
    private long userID; //user unqiue id
    private String token; //JWT token 

}