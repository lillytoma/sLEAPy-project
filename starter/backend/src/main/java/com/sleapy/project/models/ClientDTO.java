package com.sleapy.project.models;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientDTO {

    private Long id;
    private String email;
    // private String name;
    private double balance;
    
}
