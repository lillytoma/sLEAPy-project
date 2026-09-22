package com.sleapy.project.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "clients")
@Data
public class ClientEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Long id; // Primary key

    @Column(name = "email", nullable = false, unique = true)
    private String email; // Unique email used for authentication

    @Column(name = "balance")
    private double cashBalance; // client's account balance

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Bcrypt hashed password

    @Column(name = "address", nullable = false)
    private String address; // Residential address

    @Column(name = "phone_number")
    private String phoneNumber; // Contact phone number
    
    @Column(name = "ssn")
    private String ssn; // Social security number (encrypted)

    @ManyToOne 
    @JoinColumn(name = "clientstatus_id")
    private ClientStatus clientStatus; // foreign key to ClientStatus entity

}
