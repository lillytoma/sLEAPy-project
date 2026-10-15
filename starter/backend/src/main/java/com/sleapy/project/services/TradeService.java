package com.sleapy.project.services;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
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
    private final OrderStatusNotificationService orderStatusNotificationService;

    @Transactional
    public TradeResponseDTO buyStock(BuyOrderRequestDTO request) {
        OrderBusinessRuleValidationService.BuyValidation validation =
                orderValidationService.validateBuy(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalCost = validation.total();

        orderStatusNotificationService.publish(null, request.getClientId(), instrument.getSymbol(),
                "BUY", "PENDING", "Buy order received");
        orderStatusNotificationService.publish(null, request.getClientId(), instrument.getSymbol(),
                "BUY", "ACCEPTED", "Buy order accepted");

        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        client.setCashBalance(cashBalance.subtract(totalCost).doubleValue());
        clientMapper.update(client);

        Optional<HoldingEntity> existingHolding = holdingMapper.findByClientIdAndInstrumentId(
                request.getClientId(), request.getInstrumentId());
        if (existingHolding.isPresent()) {
            HoldingEntity holding = existingHolding.get();
            holding.setQuantityShares(holding.getQuantityShares().add(quantity));
            holdingMapper.update(holding);
        } else {
            HoldingEntity holding = new HoldingEntity();
            holding.setClient(client);
            holding.setInstrument(instrument);
            holding.setQuantityShares(quantity);
            holding.setPurchasePrice(currentPrice);
            holdingMapper.save(holding);
        }

        OrderEntity order = createOrder(client, instrument, quantity, currentPrice, "BUY");
        orderStatusNotificationService.publish(order.getId(), request.getClientId(), instrument.getSymbol(),
                "BUY", "FILLED", "Buy order filled");

        return new TradeResponseDTO(order.getId(), request.getClientId(), instrument.getSymbol(), "BUY",
                quantity, currentPrice, totalCost, LocalDate.now(), "FILLED",
                "Successfully purchased " + quantity + " shares of " + instrument.getSymbol()
                        + " at $" + currentPrice + " per share");
    }

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

        orderStatusNotificationService.publish(null, request.getClientId(), instrument.getSymbol(),
                "SELL", "PENDING", "Sell order received");
        orderStatusNotificationService.publish(null, request.getClientId(), instrument.getSymbol(),
                "SELL", "ACCEPTED", "Sell order accepted");

        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        client.setCashBalance(cashBalance.add(totalProceeds).doubleValue());
        clientMapper.update(client);

        BigDecimal remainingQuantity = holding.getQuantityShares().subtract(quantity);
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            holdingMapper.deleteById(holding.getId());
        } else {
            holding.setQuantityShares(remainingQuantity);
            holdingMapper.update(holding);
        }

        OrderEntity order = createOrder(client, instrument, quantity, currentPrice, "SELL");
        orderStatusNotificationService.publish(order.getId(), request.getClientId(), instrument.getSymbol(),
                "SELL", "FILLED", "Sell order filled");

        return new TradeResponseDTO(order.getId(), request.getClientId(), instrument.getSymbol(), "SELL",
                quantity, currentPrice, totalProceeds, LocalDate.now(), "FILLED",
                "Successfully sold " + quantity + " shares of " + instrument.getSymbol()
                        + " at $" + currentPrice + " per share");
    }

    private OrderEntity createOrder(ClientEntity client, InstrumentEntity instrument,
                                    BigDecimal quantity, BigDecimal price, String tradeType) {
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(price);
        order.setTradeType(tradeType);
        OrderStatus filledStatus = orderStatusMapper.findById(2L)
                .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found"));
        order.setStatus(filledStatus);
        orderMapper.save(order);
        return order;
    }
}
