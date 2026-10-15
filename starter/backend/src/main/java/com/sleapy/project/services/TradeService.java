package com.sleapy.project.services;

import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.ClientMapper;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TradeService {

    private final OrderBusinessRuleValidationService orderValidationService;
    private final ClientMapper clientMapper;
    private final HoldingMapper holdingMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusMapper orderStatusMapper;

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
    @Transactional
    public TradeResponseDTO buyStock(BuyOrderRequestDTO request) {
        OrderBusinessRuleValidationService.BuyValidation validation =
                orderValidationService.validateBuy(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalCost = validation.total();
        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());

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
            BigDecimal newQuantity = holding.getQuantityShares().add(quantity);
            holding.setQuantityShares(newQuantity);
            holdingMapper.update(holding);
        } else {
            // Create new holding
            HoldingEntity newHolding = new HoldingEntity();
            newHolding.setClient(client);
            newHolding.setInstrument(instrument);
            newHolding.setQuantityShares(quantity);
            newHolding.setPurchasePrice(currentPrice);
            holdingMapper.save(newHolding);
        }

        // 3. Create order record (for transaction history)
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(currentPrice);
        order.setTradeType("BUY");
        
        // Set order status to "Filled" (ID = 2 based on seeded lookup data)
        OrderStatus filledStatus = orderStatusMapper.findById(2L)
            .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found"));
        order.setStatus(filledStatus);
        
        orderMapper.save(order);

        // Build response
        return new TradeResponseDTO(
                order.getId(),
                request.getClientId(),
                instrument.getSymbol(),
                "BUY",
                quantity,
                currentPrice,
                totalCost,
                LocalDate.now(),
                "Successfully purchased " + quantity + " shares of " + instrument.getSymbol() +
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
    @Transactional
    public TradeResponseDTO sellStock(SellOrderRequestDTO request) {
        OrderBusinessRuleValidationService.SellValidation validation =
            orderValidationService.validateSell(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        HoldingEntity holding = validation.holding();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalProceeds = validation.total();

        // Execute transaction in database
        // 1. Update client cash balance (credit)
        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        client.setCashBalance(cashBalance.add(totalProceeds).doubleValue());
        clientMapper.update(client);

        // 2. Update holding - reduce quantity or delete if zero
        BigDecimal newQuantity = holding.getQuantityShares().subtract(quantity);
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
        order.setQuantity(quantity);
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(currentPrice);
        order.setTradeType("SELL");
        
        // Set order status to "Filled" (ID = 2 based on seeded lookup data)
        OrderStatus filledStatus = orderStatusMapper.findById(2L)
            .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found"));
        order.setStatus(filledStatus);
        
        orderMapper.save(order);

        // Build response
        return new TradeResponseDTO(
                order.getId(),
                request.getClientId(),
                instrument.getSymbol(),
                "SELL",
                quantity,
                currentPrice,
                totalProceeds,
                LocalDate.now(),
                "Successfully sold " + quantity + " shares of " + instrument.getSymbol() +
                        " at $" + currentPrice + " per share"
        );
    }

}
