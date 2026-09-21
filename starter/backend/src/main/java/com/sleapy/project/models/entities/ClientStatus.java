package com.sleapy.project.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity 
@Table(name = "client_status")
@Data 
public class ClientStatus {

    @Id
    @Column(name = "clientstatus_Id")
     private Long id;

     @Column(name = "clientstatus_name")
    private String name;

  
}
