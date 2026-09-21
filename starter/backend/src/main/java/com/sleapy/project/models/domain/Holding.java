package com.sleapy.project.models.domain;

import com.sleapy.project.exceptions.InsufficientSharesException;
import com.sleapy.project.models.dtos.HoldingDTO;

/**
 * Domain model representing a client's ownership of shares in a single instrument.
 * Provides calculations for current value, profit/loss, and yield.
 */
public class Holding {
    
    private final Long id; // Unique holding identifier
    private final Instrument instrument; // The financial instrument being held
    private final Client client; // The owner of this holding
    private double totalShares; // Total number of shares owned
    private double totalPrice; // Total purchase cost of all shares

    /**
     * Constructs a Holding from a DTO.
     */
    public Holding(HoldingDTO dto) {
        this.id = dto.getHoldingId();
        this.instrument = new Instrument(dto.getInstrument());
        this.client = new Client(dto.getClient());
        
        this.totalShares = dto.getTotalShares();
        this.totalPrice = dto.getTotalPrice();
    }
    
    /**
     * Calculates the current market value (shares × current price).
     */
    public double calcCurrentValue() {
        return this.totalShares * this.instrument.getCurrentMarketPrice();
    }
    
    /**
     * Calculates profit/loss (current value - purchase cost).
     */
    public double calcNetPrice() {        
        return this.calcCurrentValue() - this.totalPrice;
    }

    /**
     * Calculates percentage return on investment (net profit / purchase cost × 100).
     */
    public double calcYieldPercentage() {
        return this.calcNetPrice() / this.totalPrice * 100;
    }

    /**
     * Returns true if the holding is currently profitable.
     */
    public boolean hasPositiveNetPrice() {
        return this.calcNetPrice() > 0;
    }

    /**
     * Sets the total number of shares.
     */
    public void setTotalShares(double totalShares) {
        this.totalShares = totalShares;
    }

    /**
     * Sets the total purchase cost.
     */
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    /**
     * Adds shares to the holding (typically when acquiring more of the same instrument).
     * Throws exception if amount is negative.
     */
    public void addShares(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add cannot be negative");
        }
        this.totalShares += amount;
    }

    /**
     * Adds to the total purchase cost (typically when acquiring more shares).
     * Throws exception if amount is negative.
     */
    public void addPrice(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add cannot be negative");
        }
        this.totalPrice += amount;
    }

    /**
     * Removes shares from the holding (typically when selling).
     * Throws exception if amount is negative or exceeds available shares.
     */
    public void removeShares(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to remove cannot be negative");
        }
        if (amount > this.totalShares) {
            throw new InsufficientSharesException("Insufficient shares to remove");
        }
        this.totalShares -= amount;
    }

    /**
     * Removes from the total purchase cost (typically when selling shares).
     * Throws exception if amount is negative or exceeds total cost.
     */
    public void removePrice(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to remove cannot be negative");
        }
        if (amount > this.totalShares) {
            throw new InsufficientSharesException("Insufficient shares to remove");
        }
        this.totalPrice -= amount;
    }

    // Getters
    public Long getId() {
        return this.id;
    }

    public Instrument getInstrument() {
        return this.instrument;
    }

    public Client getClient() {
        return this.client;
    }

    public double getTotalShares() {
        return this.totalShares;
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }
}
