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
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TradeService {
    private static final ExecutorService ORDER_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final ConcurrentHashMap<String, Object> ACCEPTANCE_LOCKS = new ConcurrentHashMap<>();

    private final OrderBusinessRuleValidationService orderValidationService;
    private final ClientMapper clientMapper;
    private final HoldingMapper holdingMapper;
    private final InstrumentMapper instrumentMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusMapper orderStatusMapper;
    private final OrderStatusNotificationService orderStatusNotificationService;
    private final OrderAuditLogService orderAuditLogService;
    private final PriceService priceService;
    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Transactional
    public TradeResponseDTO submitBuyOrder(BuyOrderRequestDTO request) {
        PendingOrderDetails details = preparePendingOrder(request.getClientId(), request.getInstrumentId(),
                request.getQuantity());
        OrderEntity order = createPendingOrder(details.client(), details.instrument(),
                details.quantity(), details.price(), "BUY");
        publishAccepted(order, request.getClientId(), details.instrument().getSymbol(), "BUY");
        scheduleExecution(order.getId(), request.getClientId(), request.getInstrumentId(),
                details.quantity(), details.instrument().getSymbol(), "BUY",
                details.client().getUsername(), details.total());
        return pendingResponse(order, request.getClientId(), details.instrument().getSymbol(), "BUY",
                details.quantity(), details.price(), details.total());
    }

    @Transactional
    public TradeResponseDTO submitSellOrder(SellOrderRequestDTO request) {
        PendingOrderDetails details = preparePendingOrder(request.getClientId(), request.getInstrumentId(),
                request.getQuantity());
        OrderEntity order = createPendingOrder(details.client(), details.instrument(),
                details.quantity(), details.price(), "SELL");
        publishAccepted(order, request.getClientId(), details.instrument().getSymbol(), "SELL");
        scheduleExecution(order.getId(), request.getClientId(), request.getInstrumentId(),
                details.quantity(), details.instrument().getSymbol(), "SELL",
                details.client().getUsername(), details.total());
        return pendingResponse(order, request.getClientId(), details.instrument().getSymbol(), "SELL",
                details.quantity(), details.price(), details.total());
    }

    private PendingOrderDetails preparePendingOrder(Long clientId, Long instrumentId, BigDecimal requestedQuantity) {
        if (requestedQuantity == null || requestedQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        ClientEntity client = clientMapper.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));
        InstrumentEntity instrument = instrumentMapper.findById(instrumentId)
                .orElseThrow(() -> new NoSuchElementException("Instrument not found"));
        BigDecimal quantity = requestedQuantity.setScale(3, RoundingMode.HALF_UP);
        BigDecimal price = priceService.getCurrentPrice(instrument.getSymbol());
        return new PendingOrderDetails(client, instrument, quantity, price, price.multiply(quantity));
    }

    private void scheduleExecution(Long orderId, Long clientId, Long instrumentId,
                                   BigDecimal quantity, String symbol, String tradeType,
                                   String username, BigDecimal totalAmount) {
        Runnable acceptanceStage = () -> acceptPendingOrder(orderId, clientId, instrumentId,
                quantity, symbol, tradeType, username, totalAmount);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ORDER_EXECUTOR.execute(acceptanceStage);
                }
            });
        } else {
            ORDER_EXECUTOR.execute(acceptanceStage);
        }
    }

    private void acceptPendingOrder(Long orderId, Long clientId, Long instrumentId,
                                    BigDecimal quantity, String symbol, String tradeType,
                                    String username, BigDecimal totalAmount) {
        Object acceptanceLock = ACCEPTANCE_LOCKS.computeIfAbsent(
                clientId + ":" + instrumentId, ignored -> new Object());
        synchronized (acceptanceLock) {
            try {
                if ("BUY".equals(tradeType)) {
                    orderValidationService.validateBuy(clientId, instrumentId, quantity);
                } else {
                    orderValidationService.validateSell(clientId, instrumentId, quantity, orderId);
                }

                orderStatusNotificationService.publish(orderId, clientId, symbol, tradeType,
                        "ACCEPTED", "Order accepted and queued for execution");
                orderAuditLogService.log(orderId, username, tradeType, symbol, totalAmount,
                        quantity, "ACCEPTED");

                Runnable fillStage = () -> executePendingOrder(orderId, clientId, instrumentId,
                        quantity, tradeType);
                ORDER_EXECUTOR.execute(fillStage);
            } catch (RuntimeException exception) {
                rejectPendingOrder(orderId, clientId, tradeType, exception.getMessage());
            }
        }
    }

    private record PendingOrderDetails(ClientEntity client, InstrumentEntity instrument,
                                       BigDecimal quantity, BigDecimal price, BigDecimal total) {
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
                            orderValidationService.validateSell(clientId, instrumentId, quantity, orderId);
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
        orderAuditLogService.log(orderId, client.getUsername(), "BUY", instrument.getSymbol(),
                totalCost, quantity, "FILLED");
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
        orderAuditLogService.log(orderId, client.getUsername(), "SELL", instrument.getSymbol(),
                validation.total(), quantity, "FILLED");
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
        orderAuditLogService.log(order.getId(), order.getClient().getUsername(), tradeType, symbol,
                order.getPurchasePrice().multiply(order.getQuantity()),
                order.getQuantity(), "PENDING");
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
        Runnable rejection = () -> {
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
            orderAuditLogService.log(order.getId(), order.getClient().getUsername(), tradeType,
                    order.getInstrument().getSymbol(), order.getPurchasePrice().multiply(order.getQuantity()),
                    order.getQuantity(), "REJECTED");
            orderStatusNotificationService.publish(orderId, clientId,
                    order.getInstrument().getSymbol(), tradeType, "REJECTED",
                    message == null ? "Order execution failed" : message);
        };
        if (transactionTemplate == null) {
            rejection.run();
        } else {
            transactionTemplate.executeWithoutResult(status -> rejection.run());
        }
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
