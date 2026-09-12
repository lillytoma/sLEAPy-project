package com.sleapy.project.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class ClientDTO {

    private Long id;
    private String email;
    // private String name;
    private double balance;
    
}
