package com.sleapy.project.models;

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
    private Long id; // primary key

    @Column(name = "email", nullable = false, unique = true)
    private String email; // unique email for each client

    @Column(name = "balance")
    private double balance; // client's account balance

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // hashed password for security

    @Column(name = "address", nullable = false)
    private String address; // client's address

    @Column(name = "phone_number")
    private String phoneNumber; // client's phone number
    
    @Column(name = "ssn")
    private String ssn; // client's social security number

    @Column(name = "ClientStatus_ID")
    private Long clientStatusId; // foreign key to ClientStatus entity




}
