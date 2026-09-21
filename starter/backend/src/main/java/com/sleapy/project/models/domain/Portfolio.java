package com.sleapy.project.models.domain;

import java.util.ArrayList;
import java.util.HashMap;

import com.sleapy.project.models.dtos.HoldingDTO;

/**
 * Domain model representing a client's complete investment portfolio.
 * Aggregates holdings by instrument and provides portfolio-level calculations.
 */
public class Portfolio {
    
    private HashMap<String, Holding> holdingsMap; // Holdings keyed by instrument symbol

    /**
     * Constructs a Portfolio from a list of HoldingDTOs.
     */
    public Portfolio(ArrayList<HoldingDTO> holdings) {
        this.holdingsMap = new HashMap<>();

        for (HoldingDTO dto : holdings) {
            this.addHolding(dto);
        }
    }

    /**
     * Calculates total profit/loss across all holdings.
     */
    public double sumOfNetBalance() {
        double sum = 0.0;

        for (Holding h : holdingsMap.values()) {
            sum += h.calcNetPrice();
        }

        return sum;
    }

    /**
     * Calculates total current market value of all holdings.
     */
    public double sumOfTotalBalance() {
        double sum = 0.0;

        for (Holding h : holdingsMap.values()) {
            sum += h.calcCurrentValue();
        }

        return sum;
    }

    /**
     * Calculates portfolio-level yield percentage.
     */
    public double getYieldPercentage() {
        return this.sumOfNetBalance() / this.sumOfTotalBalance();
    }

    /**
     * Adds or updates a holding. If the instrument already exists, shares and cost are merged.
     */
    public void addHolding(HoldingDTO dto) {
        String instrumentId = dto.getInstrument().getSymbolId();
        Holding holding = this.holdingsMap.get(instrumentId); // null if key does not exist

        if (holding == null){
            holding = new Holding(dto);
            this.holdingsMap.put(instrumentId, holding);
        }
        else {
            // Update quantity and purchase price
            holding.addShares(dto.getTotalShares());
            holding.addPrice(dto.getTotalPrice());
        }
       
    }

    /**
     * Returns all holdings as an array.
     */
    public Holding[] convertHoldingsToArray() {
        Holding[] arr  = new Holding[this.holdingsMap.size()];
        int i = 0;

        for (Holding h : this.holdingsMap.values()) {
            arr[i++] = h;
        }

        return arr;
    }

    /**
     * Returns the map of holdings keyed by instrument symbol.
     */
    public HashMap<String, Holding> getHoldingsMap() {
        return this.holdingsMap;
    }

}
