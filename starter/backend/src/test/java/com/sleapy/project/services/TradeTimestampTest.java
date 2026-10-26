package com.sleapy.project.services;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.mappers.OrderStatusMapper;
import com.sleapy.project.models.dtos.BuyOrderRequestDTO;
import com.sleapy.project.models.dtos.SellOrderRequestDTO;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.models.entities.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeTimestampTest {
    @Mock private OrderBusinessRuleValidationService validation;
    @Mock private ClientMapper clients;
    @Mock private HoldingMapper holdings;
    @Mock private OrderMapper orders;
    @Mock private OrderStatusMapper statuses;
    @Mock private OrderStatusNotificationService notifications;
    private TradeService trades;
    private final ClientEntity client = new ClientEntity();
    private final InstrumentEntity instrument = new InstrumentEntity();

    @BeforeEach
    void setUp() {
        trades = new TradeService(validation, clients, holdings, orders, statuses, notifications);
        client.setCashBalance(1000);
        instrument.setSymbol("ABC");
        OrderStatus filled = new OrderStatus();
        filled.setName("Filled");
        when(statuses.findById(2L)).thenReturn(Optional.of(filled));
    }

    @Test
    void buyRecordsPlacementBeforeValidationAndFillAfterSettlement() {
        AtomicReference<LocalDateTime> validationStarted = new AtomicReference<>();
        when(validation.validateBuy(1L, 2L, BigDecimal.ONE)).thenAnswer(invocation -> {
            validationStarted.set(LocalDateTime.now());
            return new OrderBusinessRuleValidationService.BuyValidation(
                    client, instrument, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN);
        });
        when(holdings.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.empty());
        AtomicReference<LocalDateTime> settledAt = new AtomicReference<>();
        doAnswer(invocation -> {
            settledAt.set(LocalDateTime.now());
            return null;
        }).when(holdings).save(any());

        trades.buyStock(new BuyOrderRequestDTO(1L, 2L, BigDecimal.ONE));

        assertTimestamps(validationStarted.get(), settledAt.get(), "BUY");
    }

    @Test
    void sellRecordsPlacementBeforeValidationAndFillAfterSettlement() {
        HoldingEntity holding = new HoldingEntity();
        holding.setQuantityShares(BigDecimal.ONE);
        holding.setId(3L);
        AtomicReference<LocalDateTime> validationStarted = new AtomicReference<>();
        when(validation.validateSell(1L, 2L, BigDecimal.ONE)).thenAnswer(invocation -> {
            validationStarted.set(LocalDateTime.now());
            return new OrderBusinessRuleValidationService.SellValidation(
                    client, instrument, holding, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN);
        });
        AtomicReference<LocalDateTime> settledAt = new AtomicReference<>();
        doAnswer(invocation -> {
            settledAt.set(LocalDateTime.now());
            return null;
        }).when(holdings).deleteById(3L);

        trades.sellStock(new SellOrderRequestDTO(1L, 2L, BigDecimal.ONE));

        assertTimestamps(validationStarted.get(), settledAt.get(), "SELL");
    }

    private void assertTimestamps(LocalDateTime validationStarted, LocalDateTime settledAt, String side) {
        ArgumentCaptor<OrderEntity> saved = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orders).save(saved.capture());
        OrderEntity order = saved.getValue();
        assertNotNull(order.getTimeOfPurchase());
        assertNotNull(order.getTimeFilled());
        assertFalse(order.getTimeOfPurchase().isAfter(validationStarted));
        assertFalse(order.getTimeFilled().isBefore(settledAt));
        assertFalse(order.getTimeFilled().isAfter(LocalDateTime.now()));
        assertEquals(side, order.getTradeType());
        assertEquals("Filled", order.getStatus().getName());
    }
}
