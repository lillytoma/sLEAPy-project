package com.sleapy.project.services;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.mappers.OrderStatusMapper;
import com.sleapy.project.exceptions.InsufficientCashException;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TradeService {
    private static final long ACCEPT_DELAY_SECONDS = 15;
    private static final long FILL_DELAY_SECONDS = 30;
    private static final ScheduledExecutorService ORDER_EXECUTOR =
            Executors.newScheduledThreadPool(2);

    private final OrderBusinessRuleValidationService orderValidationService;
    private final ClientMapper clientMapper;
    private final HoldingMapper holdingMapper;
    private final InstrumentMapper instrumentMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusMapper orderStatusMapper;
    private final OrderStatusNotificationService orderStatusNotificationService;
    private final PriceService priceService;
    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Transactional
    public TradeResponseDTO submitBuyOrder(BuyOrderRequestDTO request) {
        OrderBusinessRuleValidationService.BuyValidation validation =
                orderValidationService.validateBuy(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        OrderEntity order = createPendingOrder(validation.client(), validation.instrument(),
                validation.quantity(), validation.price(), "BUY");
        publishAccepted(order, request.getClientId(), validation.instrument().getSymbol(), "BUY");
        scheduleExecution(order.getId(), request.getClientId(), request.getInstrumentId(),
                validation.quantity(), validation.instrument().getSymbol(), "BUY");
        return pendingResponse(order, request.getClientId(), validation.instrument().getSymbol(), "BUY",
                validation.quantity(), validation.price(), validation.total());
    }

    @Transactional
    public TradeResponseDTO submitSellOrder(SellOrderRequestDTO request) {
        OrderBusinessRuleValidationService.SellValidation validation =
                orderValidationService.validateSell(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        OrderEntity order = createPendingOrder(validation.client(), validation.instrument(),
                validation.quantity(), validation.price(), "SELL");
        publishAccepted(order, request.getClientId(), validation.instrument().getSymbol(), "SELL");
        scheduleExecution(order.getId(), request.getClientId(), request.getInstrumentId(),
                validation.quantity(), validation.instrument().getSymbol(), "SELL");
        return pendingResponse(order, request.getClientId(), validation.instrument().getSymbol(), "SELL",
                validation.quantity(), validation.price(), validation.total());
    }

    private void scheduleExecution(Long orderId, Long clientId, Long instrumentId,
                                   BigDecimal quantity, String symbol, String tradeType) {
        Runnable acceptedNotification = () -> orderStatusNotificationService.publish(
                orderId, clientId, symbol, tradeType, "ACCEPTED",
                "Order accepted and queued for execution");
        Runnable execution = () -> executePendingOrder(orderId, clientId, instrumentId, quantity, tradeType);
        Runnable schedule = () -> {
            ORDER_EXECUTOR.schedule(acceptedNotification, ACCEPT_DELAY_SECONDS, TimeUnit.SECONDS);
            ORDER_EXECUTOR.schedule(execution, FILL_DELAY_SECONDS, TimeUnit.SECONDS);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
        } else {
            schedule.run();
        }
    }

    public void executePendingOrder(Long orderId, Long clientId, Long instrumentId,
                                    BigDecimal quantity, String tradeType) {
        try {
            Runnable settlement = () -> {
                if ("BUY".equals(tradeType)) {
                    BuyOrderRequestDTO request = new BuyOrderRequestDTO(clientId, instrumentId, quantity);
                    OrderBusinessRuleValidationService.BuyValidation validation =
                            orderValidationService.validateBuy(clientId, instrumentId, quantity);
                    settlePendingBuy(orderId, request, validation);
                } else {
                    SellOrderRequestDTO request = new SellOrderRequestDTO(clientId, instrumentId, quantity);
                    OrderBusinessRuleValidationService.SellValidation validation =
                            orderValidationService.validateSell(clientId, instrumentId, quantity);
                    settlePendingSell(orderId, request, validation);
                }
            };
            if (transactionTemplate == null) {
                settlement.run();
            } else {
                transactionTemplate.executeWithoutResult(status -> settlement.run());
            }
        } catch (RuntimeException exception) {
            rejectPendingOrder(orderId, clientId, tradeType, exception.getMessage());
        }
    }

    private void settlePendingBuy(Long orderId, BuyOrderRequestDTO request,
                                  OrderBusinessRuleValidationService.BuyValidation validation) {
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalCost = validation.total();
        client.setCashBalance(BigDecimal.valueOf(client.getCashBalance()).subtract(totalCost).doubleValue());
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
        fillOrder(orderId, currentPrice);
        orderStatusNotificationService.publish(orderId, request.getClientId(), instrument.getSymbol(),
                "BUY", "FILLED", "Buy order filled");
    }

    private void settlePendingSell(Long orderId, SellOrderRequestDTO request,
                                   OrderBusinessRuleValidationService.SellValidation validation) {
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        HoldingEntity holding = validation.holding();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        client.setCashBalance(BigDecimal.valueOf(client.getCashBalance()).add(validation.total()).doubleValue());
        clientMapper.update(client);

        BigDecimal remainingQuantity = holding.getQuantityShares().subtract(quantity);
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            holdingMapper.deleteById(holding.getId());
        } else {
            holding.setQuantityShares(remainingQuantity);
            holdingMapper.update(holding);
        }
        fillOrder(orderId, currentPrice);
        orderStatusNotificationService.publish(orderId, request.getClientId(), instrument.getSymbol(),
                "SELL", "FILLED", "Sell order filled");
    }

    private OrderEntity createPendingOrder(ClientEntity client, InstrumentEntity instrument,
                                           BigDecimal quantity, BigDecimal price, String tradeType) {
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        order.setTimeOfPurchase(LocalDate.now());
        order.setPurchasePrice(price);
        order.setTradeType(tradeType);
        order.setStatus(orderStatusMapper.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Order status ID 1 (Pending) not found")));
        orderMapper.save(order);
        return order;
    }

    private void publishAccepted(OrderEntity order, Long clientId, String symbol, String tradeType) {
        orderStatusNotificationService.publish(order.getId(), clientId, symbol, tradeType,
                "PENDING", "Order received and awaiting execution");
    }

    private TradeResponseDTO pendingResponse(OrderEntity order, Long clientId, String symbol,
                                             String tradeType, BigDecimal quantity, BigDecimal price,
                                             BigDecimal total) {
        return new TradeResponseDTO(order.getId(), clientId, symbol, tradeType, quantity, price, total,
                LocalDate.now(), "PENDING", "Order received and awaiting execution");
    }

    private void fillOrder(Long orderId, BigDecimal executionPrice) {
        OrderEntity order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalStateException("Pending order not found: " + orderId);
        }
        order.setPurchasePrice(executionPrice);
        order.setTimeFilled(LocalDate.now());
        order.setStatus(orderStatusMapper.findById(2L)
                .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found")));
        orderMapper.update(order);
    }

    private void rejectPendingOrder(Long orderId, Long clientId, String tradeType, String message) {
        OrderEntity order = orderMapper.findById(orderId);
        if (order == null) return;
        OrderStatus status = orderStatusMapper.findById(3L).orElse(null);
        if (status == null) {
            status = new OrderStatus();
            status.setId(2L);
            status.setName("Rejected");
        }
        order.setStatus(status);
        orderMapper.update(order);
        orderStatusNotificationService.publish(orderId, clientId,
                order.getInstrument().getSymbol(), tradeType, "REJECTED",
                message == null ? "Order execution failed" : message);
    }

    @Transactional
    public TradeResponseDTO buyStock(BuyOrderRequestDTO request) {
        OrderBusinessRuleValidationService.BuyValidation validation = validateBuy(request);
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalCost = validation.total();
        BigDecimal displayPrice = currentPrice.setScale(3, RoundingMode.HALF_UP);

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
                        + " at $" + displayPrice + " per share");
    }

    @Transactional
    public TradeResponseDTO sellStock(SellOrderRequestDTO request) {
        OrderBusinessRuleValidationService.SellValidation validation = validateSell(request);
        ClientEntity client = validation.client();
        InstrumentEntity instrument = validation.instrument();
        HoldingEntity holding = validation.holding();
        BigDecimal quantity = validation.quantity();
        BigDecimal currentPrice = validation.price();
        BigDecimal totalProceeds = validation.total();
        BigDecimal displayPrice = currentPrice.setScale(3, RoundingMode.HALF_UP);

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
                        + " at $" + displayPrice + " per share");
    }

    private OrderBusinessRuleValidationService.BuyValidation validateBuy(BuyOrderRequestDTO request) {
        if (orderValidationService != null) {
            return orderValidationService.validateBuy(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        }
        ClientEntity client = clientMapper.findById(request.getClientId()).orElseThrow(
                () -> new NoSuchElementException("Client not found"));
        InstrumentEntity instrument = instrumentMapper.findById(request.getInstrumentId()).orElseThrow(
                () -> new NoSuchElementException("Instrument not found"));
        BigDecimal quantity = request.getQuantity();
        BigDecimal price = priceService.getCurrentPrice(instrument.getSymbol());
        BigDecimal total = price.multiply(quantity);
        if (BigDecimal.valueOf(client.getCashBalance()).compareTo(total) < 0) {
            throw new InsufficientCashException("Insufficient cash balance");
        }
        return new OrderBusinessRuleValidationService.BuyValidation(client, instrument, quantity, price, total);
    }

    private OrderBusinessRuleValidationService.SellValidation validateSell(SellOrderRequestDTO request) {
        if (orderValidationService != null) {
            return orderValidationService.validateSell(request.getClientId(), request.getInstrumentId(), request.getQuantity());
        }
        ClientEntity client = clientMapper.findById(request.getClientId()).orElseThrow(
                () -> new NoSuchElementException("Client not found"));
        InstrumentEntity instrument = instrumentMapper.findById(request.getInstrumentId()).orElseThrow(
                () -> new NoSuchElementException("Instrument not found"));
        HoldingEntity holding = holdingMapper.findByClientIdAndInstrumentId(request.getClientId(), request.getInstrumentId())
                .orElseThrow(() -> new NoSuchElementException("Position not found"));
        BigDecimal price = priceService.getCurrentPrice(instrument.getSymbol());
        return new OrderBusinessRuleValidationService.SellValidation(client, instrument, holding,
                request.getQuantity(), price, price.multiply(request.getQuantity()));
    }

    private OrderEntity createOrder(ClientEntity client, InstrumentEntity instrument,
                                    BigDecimal quantity, BigDecimal price, String tradeType) {
        OrderEntity order = new OrderEntity();
        order.setClient(client);
        order.setInstrument(instrument);
        order.setQuantity(quantity);
        order.setTimeOfPurchase(LocalDate.now());
        order.setTimeFilled(LocalDate.now());
        order.setPurchasePrice(price);
        order.setTradeType(tradeType);
        OrderStatus filledStatus = orderStatusMapper.findById(2L)
                .orElseThrow(() -> new IllegalStateException("Order status ID 2 (Filled) not found"));
        order.setStatus(filledStatus);
        orderMapper.save(order);
        return order;
    }
}
