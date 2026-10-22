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
import com.sleapy.project.exceptions.OrderValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final MarketHoursService marketHoursService;

    @Transactional
    public TradeResponseDTO buyStock(BuyOrderRequestDTO request) {
        clientMapper.lockForTrading(request.getClientId());
        if (!marketHoursService.isMarketOpen()) {
            var validation = orderValidationService.validateBuy(
                    request.getClientId(), request.getInstrumentId(), request.getQuantity(), false);
            return queue(validation.client(), validation.instrument(), validation.quantity(), "BUY");
        }
        OrderBusinessRuleValidationService.BuyValidation validation =
                orderValidationService.validateBuy(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        return settleBuy(validation, null);
    }

    private TradeResponseDTO settleBuy(OrderBusinessRuleValidationService.BuyValidation validation, OrderEntity queued) {
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalCost = validation.total();
        BigDecimal displayPrice = currentPrice.setScale(3, RoundingMode.HALF_UP);
        if (!marketHoursService.isMarketOpen()) {
            if (queued == null) return queue(client, instrument, quantity, "BUY");
            throw new OrderValidationException("MARKET_CLOSED", "Regular session closed before execution");
        }

        orderStatusNotificationService.publish(null, client.getId(), instrument.getSymbol(),
                "BUY", "PENDING", "Buy order received");
        orderStatusNotificationService.publish(null, client.getId(), instrument.getSymbol(),
                "BUY", "ACCEPTED", "Buy order accepted");

        BigDecimal cashBalance = BigDecimal.valueOf(client.getCashBalance());
        client.setCashBalance(cashBalance.subtract(totalCost).doubleValue());
        clientMapper.update(client);

        Optional<HoldingEntity> existingHolding = holdingMapper.findByClientIdAndInstrumentId(
                client.getId(), instrument.getId());
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

        OrderEntity order = saveFilledOrder(client, instrument, quantity, currentPrice, "BUY", queued);
        orderStatusNotificationService.publish(order.getId(), client.getId(), instrument.getSymbol(),
                "BUY", "FILLED", "Buy order filled");

        return new TradeResponseDTO(order.getId(), client.getId(), instrument.getSymbol(), "BUY",
                quantity, currentPrice, totalCost, sessionDate(), "FILLED",
                "Successfully purchased " + quantity + " shares of " + instrument.getSymbol()
                        + " at $" + displayPrice + " per share");
    }

    @Transactional
    public TradeResponseDTO sellStock(SellOrderRequestDTO request) {
        clientMapper.lockForTrading(request.getClientId());
        if (!marketHoursService.isMarketOpen()) {
            var validation = orderValidationService.validateSell(
                    request.getClientId(), request.getInstrumentId(), request.getQuantity(), false);
            return queue(validation.client(), validation.instrument(), validation.quantity(), "SELL");
        }
        OrderBusinessRuleValidationService.SellValidation validation =
                orderValidationService.validateSell(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        return settleSell(validation, null);
    }

    private TradeResponseDTO settleSell(OrderBusinessRuleValidationService.SellValidation validation, OrderEntity queued) {
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        HoldingEntity holding = validation.holding();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalProceeds = validation.total();
        BigDecimal displayPrice = currentPrice.setScale(3, RoundingMode.HALF_UP);
        if (!marketHoursService.isMarketOpen()) {
            if (queued == null) return queue(client, instrument, quantity, "SELL");
            throw new OrderValidationException("MARKET_CLOSED", "Regular session closed before execution");
        }

        orderStatusNotificationService.publish(null, client.getId(), instrument.getSymbol(),
                "SELL", "PENDING", "Sell order received");
        orderStatusNotificationService.publish(null, client.getId(), instrument.getSymbol(),
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

        OrderEntity order = saveFilledOrder(client, instrument, quantity, currentPrice, "SELL", queued);
        orderStatusNotificationService.publish(order.getId(), client.getId(), instrument.getSymbol(),
                "SELL", "FILLED", "Sell order filled");

        return new TradeResponseDTO(order.getId(), client.getId(), instrument.getSymbol(), "SELL",
                quantity, currentPrice, totalProceeds, sessionDate(), "FILLED",
                "Successfully sold " + quantity + " shares of " + instrument.getSymbol()
                        + " at $" + displayPrice + " per share");
    }

    private OrderEntity saveFilledOrder(ClientEntity client, InstrumentEntity instrument,
                                    BigDecimal quantity, BigDecimal price, String tradeType, OrderEntity queued) {
        OrderEntity order = queued == null ? new OrderEntity() : queued;
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        if (queued == null) order.setTimeOfPurchase(sessionDate());
        order.setTimeFilled(sessionDate());
        order.setPurchasePrice(price);
        order.setTradeType(tradeType);
        OrderStatus filledStatus = orderStatusMapper.findById(2L)
                .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found"));
        order.setStatus(filledStatus);
        if (queued == null) orderMapper.save(order);
        else orderMapper.update(order);
        return order;
    }

    private LocalDate sessionDate() {
        return LocalDate.now(ZoneId.of("America/New_York"));
    }

    private OrderStatus status(String name) {
        orderStatusMapper.ensureStatus(name);
        return orderStatusMapper.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Missing order status: " + name));
    }

    private TradeResponseDTO queue(ClientEntity client, InstrumentEntity instrument, BigDecimal quantity, String side) {
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        order.setTradeType(side);
        order.setTimeOfPurchase(sessionDate());
        order.setStatus(status("Queued"));
        orderMapper.save(order);
        String message = "Market order queued for the next regular Nasdaq/NYSE session. "
                + "No extended-hours execution. Price, cash and shares are rechecked at execution; "
                + "funds and shares are not reserved.";
        orderStatusNotificationService.publish(order.getId(), client.getId(), instrument.getSymbol(),
                side, "QUEUED", message);
        return new TradeResponseDTO(order.getId(), client.getId(), instrument.getSymbol(), side,
                quantity, null, null, sessionDate(), "QUEUED", message);
    }

    @Transactional
    public void executeQueued(Long orderId) {
        if (!marketHoursService.isMarketOpen() || orderMapper.lockQueued(orderId) == null) return;
        OrderEntity order = orderMapper.findById(orderId);
        clientMapper.lockForTrading(order.getClient().getId());
        try {
            if ("BUY".equals(order.getTradeType())) {
                var validation = orderValidationService.validateBuy(
                        order.getClient().getId(), order.getInstrument().getId(), order.getQuantity());
                settleBuy(validation, order);
            } else if ("SELL".equals(order.getTradeType())) {
                var validation = orderValidationService.validateSell(
                        order.getClient().getId(), order.getInstrument().getId(), order.getQuantity());
                settleSell(validation, order);
            } else {
                throw new OrderValidationException("INVALID_SIDE", "Unsupported queued order side");
            }
        } catch (OrderValidationException ex) {
            if ("MARKET_CLOSED".equals(ex.getRule()) || "INVALID_PRICE".equals(ex.getRule())) throw ex;
            order.setStatus(status("Rejected"));
            orderMapper.update(order);
            orderStatusNotificationService.publish(orderId, order.getClient().getId(), order.getInstrument().getSymbol(),
                    order.getTradeType(), "REJECTED", ex.getMessage());
        }
    }
}
