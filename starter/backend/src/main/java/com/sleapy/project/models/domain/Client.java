package com.sleapy.project.models.domain;

import com.sleapy.project.exceptions.InsufficientCashException;
import com.sleapy.project.models.dtos.ClientDTO;

/**
 * Domain model representing a client (Joanna, David, Priya).
 * Manages client identity, cash balance, and portfolio of holdings.
 */
public class Client {
    
    private final Long id; // Unique client identifier
    private final String email; // Email address (login credential)
    private final String username; // Display username
    private final String phoneNumber; // Contact phone number
    private final String ssnLast4; // Last 4 digits of social security number
    private final String fullAddress; // Residential address
    private final String birthDate; // Birth date (YYYY-MM-DD format)
    private final Portfolio portfolio; // Container of all client holdings
    private double cashBalance; // Available cash for trading

    /**
     * Constructs a Client from a DTO.
     */
    public Client(ClientDTO dto) {
        this.id = dto.getId();
        this.email = dto.getEmail();
        this.username = dto.getUsername();
        this.phoneNumber = dto.getPhoneNumber();
        this.ssnLast4 = dto.getSsnLast4();
        this.fullAddress = dto.getFullAddress();
        this.birthDate = dto.getBirthDate();
        this.portfolio = new Portfolio(dto.getHoldings());

        this.cashBalance = dto.getCashBalance();
    }

    /**
     * Directly sets the cash balance.
     */
    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    /**
     * Adds cash to the client's balance. Throws exception if amount is negative.
     */
    public void addCash(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add cannot be negative");
        }
        this.cashBalance += amount;
    }

    /**
     * Withdraws cash from the client's balance.
     * Throws exception if amount is negative or exceeds available balance.
     */
    public void withdrawCash(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to withdraw cannot be negative");
        }
        if (amount > this.cashBalance) {
            throw new InsufficientCashException("Insufficient cash balance");
        }
        this.cashBalance -= amount;
    }

    // Getters
    public Long getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUsername() {
        return this.username;
    }

    public double getCashBalance() {
        return this.cashBalance;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getSsnLast4() {
        return this.ssnLast4;
    }

    public String getFullAddress() {
        return this.fullAddress;
    }

    public String getBirthDate() {
        return this.birthDate;
    }

    public Portfolio getPortfolio() {
        return this.portfolio;
    }

}
