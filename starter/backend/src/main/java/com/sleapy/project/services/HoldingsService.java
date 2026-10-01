package com.sleapy.project.services;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.dtos.CreateHoldingRequestDTO;
import com.sleapy.project.models.dtos.UpdateHoldingRequestDTO;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.utils.HoldingDTOMapper;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@CrossOrigin(origins = "http://localhost:4200")
public class HoldingsService {
    private final HoldingMapper holdingMapper;
    private final ClientMapper clientMapper;
    private final InstrumentMapper instrumentMapper;

    public HoldingsService(HoldingMapper holdingMapper, ClientMapper clientMapper, InstrumentMapper instrumentMapper) {
        this.holdingMapper = holdingMapper;
        this.clientMapper = clientMapper;
        this.instrumentMapper = instrumentMapper;
    }

    /**
     * Retrieve all holdings for a specific client.
     * @param clientId the ID of the client
     * @return List of HoldingDTO objects for the client
     */
    public List<HoldingDTO> getClientHoldings(Long clientId) {
        List<HoldingEntity> holdings = holdingMapper.findByClientId(clientId);
        return holdings.stream()
                .map(HoldingDTOMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calculate client's total net worth (holdings market value + cash balance).
     * Net Worth = Sum(quantity × current_price) for all holdings + cash_balance
     * 
     * @param clientId the ID of the client
     * @return Map containing net worth breakdown
     * @throws NoSuchElementException if client not found
     */
    public Map<String, Object> getClientNetWorth(Long clientId) {
        List<HoldingEntity> holdings = holdingMapper.findByClientId(clientId);
        ClientEntity client = clientMapper.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + clientId));
        
        // Calculate total holdings value
        BigDecimal holdingsValue = BigDecimal.ZERO;
        for (HoldingEntity holding : holdings) {
            BigDecimal quantity = holding.getQuantityShares();
            BigDecimal currentPrice = holding.getInstrument().getCurrentPrice();
            if (currentPrice != null) {
                holdingsValue = holdingsValue.add(quantity.multiply(currentPrice));
            }
        }
        
        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        BigDecimal netWorth = holdingsValue.add(cashBalance);
        
        return Map.of(
            "clientId", clientId,
            "holdingsValue", holdingsValue,
            "cashBalance", cashBalance,
            "totalNetWorth", netWorth,
            "holdings", getClientHoldings(clientId)
        );
    }

    /**
     * Create a new holding for a client.
     * 
     * @param request the create holding request containing clientId, instrumentId, quantity, purchasePrice
     * @return HoldingDTO of the newly created holding
     * @throws NoSuchElementException if client or instrument not found
     * @throws IllegalArgumentException if input is invalid
     */
    public HoldingDTO createHolding(CreateHoldingRequestDTO request) {
        // Validate inputs
        if (request.getClientId() == null || request.getClientId() <= 0) {
            throw new IllegalArgumentException("Valid client ID required");
        }
        if (request.getInstrumentId() == null || request.getInstrumentId() <= 0) {
            throw new IllegalArgumentException("Valid instrument ID required");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (request.getPurchasePrice() == null || request.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        
        // Fetch client and instrument
        ClientEntity client = clientMapper.findById(request.getClientId())
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + request.getClientId()));
        
        InstrumentEntity instrument = instrumentMapper.findById(request.getInstrumentId())
                .orElseThrow(() -> new NoSuchElementException("Instrument not found with ID: " + request.getInstrumentId()));
        
        // Create holding entity
        HoldingEntity holding = new HoldingEntity();
        holding.setClient(client);
        holding.setInstrument(instrument);
        holding.setQuantityShares(request.getQuantity());
        holding.setPurchasePrice(request.getPurchasePrice());
        
        // Save to database
        holdingMapper.save(holding);
        
        // Convert and return as DTO
        return HoldingDTOMapper.toDTO(holding);
    }

    /**
     * Update an existing holding.
     * 
     * @param holdingId the ID of the holding to update
     * @param request the update request containing new quantity and purchasePrice
     * @return HoldingDTO of the updated holding
     * @throws NoSuchElementException if holding not found
     * @throws IllegalArgumentException if input is invalid
     */
    public HoldingDTO updateHolding(Long holdingId, UpdateHoldingRequestDTO request) {
        // Validate holding ID
        if (holdingId == null || holdingId <= 0) {
            throw new IllegalArgumentException("Valid holding ID required");
        }
        
        // Validate inputs
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (request.getPurchasePrice() == null || request.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        
        // Fetch existing holding
        HoldingEntity holding = holdingMapper.findById(holdingId)
                .orElseThrow(() -> new NoSuchElementException("Holding not found with ID: " + holdingId));
        
        // Update fields
        holding.setQuantityShares(request.getQuantity());
        holding.setPurchasePrice(request.getPurchasePrice());
        
        // Save changes
        holdingMapper.update(holding);
        
        // Convert and return as DTO
        return HoldingDTOMapper.toDTO(holding);
    }

    /**
     * Delete a holding by ID.
     * 
     * @param holdingId the ID of the holding to delete
     * @throws NoSuchElementException if holding not found
     * @throws IllegalArgumentException if holding ID is invalid
     */
    public void deleteHolding(Long holdingId) {
        // Validate holding ID
        if (holdingId == null || holdingId <= 0) {
            throw new IllegalArgumentException("Valid holding ID required");
        }
        
        // Verify holding exists
        HoldingEntity holding = holdingMapper.findById(holdingId)
                .orElseThrow(() -> new NoSuchElementException("Holding not found with ID: " + holdingId));
        
        // Delete from database
        holdingMapper.deleteById(holdingId);
    }
}
