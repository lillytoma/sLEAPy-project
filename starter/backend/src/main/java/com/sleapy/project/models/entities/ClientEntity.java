package com.sleapy.project.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class ClientEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Long id; // primary key

    @Column(name = "email", nullable = false, unique = true)
    private String email; // unique email for each client

    @Column(name = "cash_balance")
    private double balance; // client's account balance

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // hashed password for security

    @Column(name = "address", nullable = false)
    private String address; // client's address

    @Column(name = "phone_number")
    private String phoneNumber; // client's phone number
    
    @Column(name = "ssn")
    private String ssn; // client's social security number

    @Column(name = "clientstatus_id")
    private Long clientStatusId; // foreign key to ClientStatus entity




}
