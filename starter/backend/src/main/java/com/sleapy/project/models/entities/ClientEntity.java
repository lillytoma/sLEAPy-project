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

/**
 * JPA entity representing a client record in the database.
 * Persists client identity, credentials, and account information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class ClientEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Long id; // Primary key

    @Column(name = "email", nullable = false, unique = true)
    private String email; // Unique email used for authentication

    @Column(name = "cash_balance")
    private double cashBalance; // Available cash balance in account

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Bcrypt hashed password

    @Column(name = "address", nullable = false)
    private String address; // Residential address

    @Column(name = "phone_number")
    private String phoneNumber; // Contact phone number
    
    @Column(name = "ssn")
    private String ssn; // Social security number (encrypted)

    @Column(name = "clientstatus_id")
    private Long clientStatusId; // Foreign key to account status

}
