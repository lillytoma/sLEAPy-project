package com.sleapy.project.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

//BR-07
@Table(name = "Order_Status")
@Entity 
@Data 
@NoArgsConstructor 

public class OrderStatus{
    // SUBMITTED(1),
    // ACCEPTED(2),
    // FILLED(3),
    // REJECTED(4);
    

    @Id
    @Column(name = "OrderStatus_Id")
    private Long id;

    @Column(name = "OrderStatus_Name")
   private String name;
}
