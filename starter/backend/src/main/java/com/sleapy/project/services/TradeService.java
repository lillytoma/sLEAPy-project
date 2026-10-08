package com.sleapy.project.services;

import com.sleapy.project.exceptions.InsufficientCashException;
import com.sleapy.project.exceptions.InsufficientSharesException;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.mappers.OrderStatusMapper;
import com.sleapy.project.models.dtos.BuyOrderRequestDTO;
import com.sleapy.project.models.dtos.SellOrderRequestDTO;
import com.sleapy.project.models.dtos.TradeResponseDTO;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.models.entities.OrderStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TradeService {

    private final ClientMapper clientMapper;
    private final InstrumentMapper instrumentMapper;
    private final HoldingMapper holdingMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusMapper orderStatusMapper;
    private final PriceService priceService;

    /**
     * Execute a BUY order for the client.
     * 
     * Validations:
     * 1. Client exists and is active
     * 2. Instrument exists
     * 3. Client has sufficient cash balance
     * 4. Quantity is positive
     * 
     * @param request BuyOrderRequestDTO with clientId, instrumentId, quantity
     * @return TradeResponseDTO with order details and execution confirmation
     * @throws NoSuchElementException if client or instrument not found
     * @throws InsufficientCashException if client doesn't have enough cash
     * @throws IllegalArgumentException if quantity is invalid
     */
    public TradeResponseDTO buyStock(BuyOrderRequestDTO request) {
        // Validate request
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Fetch client and validate
        ClientEntity client = clientMapper.findById(request.getClientId())
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + request.getClientId()));

        if (client.getClientStatus() == null || !client.getClientStatus().getName().equals("Active")) {
            throw new IllegalArgumentException("Client is not active and cannot trade");
        }

        // Fetch instrument and validate
        InstrumentEntity instrument = instrumentMapper.findById(request.getInstrumentId())
                .orElseThrow(() -> new NoSuchElementException("Instrument not found with ID: " + request.getInstrumentId()));

        // TODO: Replace with actual y-finance price fetch
        BigDecimal currentPrice = priceService.getCurrentPrice(instrument.getSymbol());
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price for instrument: " + instrument.getSymbol());
        }

        // Calculate total cost of purchase
        BigDecimal totalCost = currentPrice.multiply(request.getQuantity());

        // Validate client has sufficient cash
        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        if (cashBalance.compareTo(totalCost) < 0) {
            throw new InsufficientCashException(
                    "Insufficient cash balance. Required: $" + totalCost +
                    ", Available: $" + cashBalance
            );
        }

        // Execute transaction in database
        // 1. Update client cash balance (debit)
        client.setCashBalance(cashBalance.subtract(totalCost).doubleValue());
        clientMapper.update(client);

        // 2. Update or create holding
        Optional<HoldingEntity> existingHolding = holdingMapper.findByClientIdAndInstrumentId(
                request.getClientId(),
                request.getInstrumentId()
        );

        if (existingHolding.isPresent()) {
            // Update existing holding - add quantity
            HoldingEntity holding = existingHolding.get();
            BigDecimal newQuantity = holding.getQuantityShares().add(request.getQuantity());
            holding.setQuantityShares(newQuantity);
            holdingMapper.update(holding);
        } else {
            // Create new holding
            HoldingEntity newHolding = new HoldingEntity();
            newHolding.setClient(client);
            newHolding.setInstrument(instrument);
            newHolding.setQuantityShares(request.getQuantity());
            newHolding.setPurchasePrice(currentPrice);
            holdingMapper.save(newHolding);
        }

        // 3. Create order record (for transaction history)
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(request.getQuantity().intValue());
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(currentPrice);
        
        // Set order status to "Filled" (ID = 2 based on mockdata.sql)
        OrderStatus filledStatus = new OrderStatus();
        filledStatus.setId(2L);
        order.setStatus(filledStatus);
        
        orderMapper.save(order);

        // Build response
        return new TradeResponseDTO(
                order.getId(),
                request.getClientId(),
                instrument.getSymbol(),
                "BUY",
                request.getQuantity(),
                currentPrice,
                totalCost,
                LocalDate.now(),
                "Successfully purchased " + request.getQuantity() + " shares of " + instrument.getSymbol() +
                        " at $" + currentPrice + " per share"
        );
    }

    /**
     * Execute a SELL order for the client.
     * 
     * Validations:
     * 1. Client exists and is active
     * 2. Instrument exists
     * 3. Client owns the instrument
     * 4. Client has sufficient shares to sell
     * 5. Quantity is positive
     * 
     * @param request SellOrderRequestDTO with clientId, instrumentId, quantity
     * @return TradeResponseDTO with order details and execution confirmation
     * @throws NoSuchElementException if client, instrument, or holding not found
     * @throws InsufficientSharesException if client doesn't own enough shares
     * @throws IllegalArgumentException if quantity is invalid
     */
    public TradeResponseDTO sellStock(SellOrderRequestDTO request) {
        // Validate request
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Fetch client and validate
        ClientEntity client = clientMapper.findById(request.getClientId())
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + request.getClientId()));

        if (client.getClientStatus() == null || !client.getClientStatus().getName().equals("Active")) {
            throw new IllegalArgumentException("Client is not active and cannot trade");
        }

        // Fetch instrument and validate
        InstrumentEntity instrument = instrumentMapper.findById(request.getInstrumentId())
                .orElseThrow(() -> new NoSuchElementException("Instrument not found with ID: " + request.getInstrumentId()));

        // Check if client owns this instrument
        HoldingEntity holding = holdingMapper.findByClientIdAndInstrumentId(
                request.getClientId(),
                request.getInstrumentId()
        ).orElseThrow(() -> new NoSuchElementException(
                "Client does not own " + instrument.getSymbol()
        ));

        // Validate client has sufficient shares
        if (holding.getQuantityShares().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientSharesException(
                    "Insufficient shares. Owned: " + holding.getQuantityShares() +
                    ", Attempting to sell: " + request.getQuantity()
            );
        }

        // TODO: Replace with actual y-finance price fetch
        BigDecimal currentPrice = priceService.getCurrentPrice(instrument.getSymbol());
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price for instrument: " + instrument.getSymbol());
        }

        // Calculate proceeds from sale
        BigDecimal totalProceeds = currentPrice.multiply(request.getQuantity());

        // Execute transaction in database
        // 1. Update client cash balance (credit)
        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        client.setCashBalance(cashBalance.add(totalProceeds).doubleValue());
        clientMapper.update(client);

        // 2. Update holding - reduce quantity or delete if zero
        BigDecimal newQuantity = holding.getQuantityShares().subtract(request.getQuantity());
        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            // Delete holding if no shares remain
            holdingMapper.deleteById(holding.getId());
        } else {
            // Update holding with reduced quantity
            holding.setQuantityShares(newQuantity);
            holdingMapper.update(holding);
        }

        // 3. Create order record (for transaction history)
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(request.getQuantity().intValue());
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(currentPrice);
        
        // Set order status to "Filled" (ID = 2 based on mockdata.sql)
        OrderStatus filledStatus = new OrderStatus();
        filledStatus.setId(2L);
        order.setStatus(filledStatus);
        
        orderMapper.save(order);

        // Build response
        return new TradeResponseDTO(
                order.getId(),
                request.getClientId(),
                instrument.getSymbol(),
                "SELL",
                request.getQuantity(),
                currentPrice,
                totalProceeds,
                LocalDate.now(),
                "Successfully sold " + request.getQuantity() + " shares of " + instrument.getSymbol() +
                        " at $" + currentPrice + " per share"
        );
    }
}
